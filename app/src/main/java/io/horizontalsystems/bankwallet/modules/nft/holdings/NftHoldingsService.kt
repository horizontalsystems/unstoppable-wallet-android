package io.horizontalsystems.bankwallet.modules.nft.holdings

import io.horizontalsystems.bankwallet.core.adapters.nft.INftAdapter
import io.horizontalsystems.bankwallet.core.managers.NftAdapterManager
import io.horizontalsystems.bankwallet.core.managers.NftMetadataManager
import io.horizontalsystems.bankwallet.core.managers.NftMetadataSyncer
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.nft.*
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.NftPrice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.rx2.collect
import java.util.concurrent.Executors

class NftHoldingsService(
    private val account: Account,
    private val nftAdapterManager: NftAdapterManager,
    private val nftMetadataManager: NftMetadataManager,
    private val nftMetadataSyncer: NftMetadataSyncer,
    private val xRateRepository: BalanceXRateRepository
) {
    var priceType: PriceType = PriceType.LastSale
        private set

    private var recordMap = mutableMapOf<BlockchainType, List<NftRecord>>()
    private var metadataMap = mutableMapOf<BlockchainType, NftAddressMetadata?>()
    private var nftItemMap = mutableMapOf<String, NftCollectionItem>()

    private val _itemsFlow = MutableStateFlow<List<Item>>(listOf())

    val itemsFlow = _itemsFlow.asStateFlow()

    suspend fun start() = withContext(Dispatchers.IO) {
        launch {
            nftAdapterManager.adaptersUpdatedFlow.collect {
                handle(it)
            }
        }

        launch {
            xRateRepository.itemObservable.collect {
                handleXRateUpdate(it)
            }
        }
    }

    private fun handleXRateUpdate(latestRates: Map<String, CoinPrice?>) {
        val items = _itemsFlow.value

        _itemsFlow.tryEmit(items.map { it.copy(assetItems = updateRates(it.assetItems, latestRates)) })
    }

    private fun updateRates(
        assets: List<AssetItem>,
        latestRates: Map<String, CoinPrice?>
    ) = assets.map { asset ->

        asset.price?.let { price ->
            latestRates[price.token.coin.uid]?.let { latestRate ->
                asset.copy(
                    priceInFiat = CurrencyValue(xRateRepository.baseCurrency, price.value.multiply(latestRate.value)),
                    coinPrice = latestRate
                )
            }
        } ?: asset
    }

    private var adaptersMapScope: CoroutineScope? = null

    @Synchronized
    private fun handle(adaptersMap: Map<NftKey, INftAdapter>) {
        recordMap.clear()
        metadataMap.clear()

        adaptersMapScope?.cancel()
        adaptersMapScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

        for ((nftKey, adapter) in adaptersMap) {
            recordMap[nftKey.blockchainType] = adapter.nftRecords
            metadataMap[nftKey.blockchainType] = nftMetadataManager.addressMetadata(nftKey)

            adaptersMapScope?.launch {
                adapter.nftRecordsFlow.collect { records ->
                    handleUpdated(records, nftKey.blockchainType)
                }
            }
        }

        adaptersMapScope?.launch {
            nftMetadataManager.addressMetadataFlow.filterNotNull().collect { (nftKey, addressMetadata) ->
                handleUpdated(addressMetadata, nftKey)
            }
        }

        syncNftItemMap()
    }

    @Synchronized
    private fun handleUpdated(addressMetadata: NftAddressMetadata, nftKey: NftKey) {
        if (account != nftKey.account) return

        metadataMap[nftKey.blockchainType] = addressMetadata

        syncNftItemMap()
    }

    @Synchronized
    private fun handleUpdated(records: List<NftRecord>, blockchainType: BlockchainType) {
        recordMap[blockchainType] = records

        syncNftItemMap()
    }

    private fun syncNftItemMap() {
        nftItemMap.clear()

        for ((blockchainType, records) in recordMap) {
            val assetMetadataMap = mutableMapOf<NftUid, NftAssetShortMetadata>()
            val collectionMetadataMap = mutableMapOf<String, NftCollectionShortMetadata>()

            metadataMap[blockchainType]?.let { addressMetadata ->
                for (meta in addressMetadata.assets) {
                    assetMetadataMap[meta.nftUid] = meta
                }
                for (meta in addressMetadata.collections) {
                    collectionMetadataMap[meta.providerUid] = meta
                }
            }

            for (record in records) {
                val assetMetadata = assetMetadataMap[record.nftUid] ?: continue
                val collectionMetadata = collectionMetadataMap[assetMetadata.providerCollectionUid] ?: continue

                val uid = assetMetadata.providerCollectionUid
                val nftItem = NftItem(record, assetMetadata)

                val collectionItem = nftItemMap[uid]
                if (collectionItem == null) {
                    nftItemMap[uid] = NftCollectionItem(collectionMetadata, listOf(nftItem))
                } else {
                    nftItemMap[uid] = collectionItem.copy(nftItems = collectionItem.nftItems + listOf(nftItem))
                }
            }
        }

        syncItems()
    }

    private fun syncItems() {
        val items = nftItemMap.map { (providerCollectionUid, nftCollectionItem) ->
            val collectionMetadata = nftCollectionItem.metadata
            Item(
                uid = providerCollectionUid,
                providerCollectionUid = collectionMetadata?.providerUid,
                imageUrl = collectionMetadata?.thumbnailImageUrl,
                name = collectionMetadata?.name ?: providerCollectionUid,
                count = nftCollectionItem.nftItems.sumOf { it.record.balance },
                assetItems = nftCollectionItem.nftItems.map { nftItem ->
                    val record = nftItem.record
                    val metadata = nftItem.assetMetadata
                    val price = when (priceType) {
                        PriceType.LastSale -> metadata?.lastSalePrice
                        PriceType.Days7 -> collectionMetadata?.averagePrice7d
                        PriceType.Days30 -> collectionMetadata?.averagePrice30
                    }
                    AssetItem(
                        nftUid = record.nftUid,
                        imageUrl = metadata?.previewImageUrl,
                        name = metadata?.displayName ?: "#${record.nftUid.tokenId}",
                        count = record.balance,
                        onSale = metadata?.onSale ?: false,
                        price = price
                    )
                }
            )
        }

        val coinUids = items.map { it.assetItems.mapNotNull { asset -> asset.price?.token?.coin?.uid } }.flatten().distinct()
        xRateRepository.setCoinUids(coinUids)

        val latestRates = xRateRepository.getLatestRates()
        val itemsWithCurrencyValues = items.map { it.copy(assetItems = updateRates(it.assetItems, latestRates)) }

        _itemsFlow.tryEmit(sort(itemsWithCurrencyValues))
    }

    private fun sort(items: List<Item>) =
        items.sortedWith { item, item2 -> item.name.compareTo(item2.name, true) }

    fun refresh() {
        nftMetadataSyncer.refresh()
    }

    fun updatePriceType(priceType: PriceType) {
        this.priceType = priceType

        syncItems()
    }

    fun stop() {
        adaptersMapScope?.cancel()
    }

    data class NftCollectionItem(
        val metadata: NftCollectionShortMetadata?,
        val nftItems: List<NftItem>
    )

    data class NftItem(
        val record: NftRecord,
        val assetMetadata: NftAssetShortMetadata?
    )

    data class Item(
        val uid: String,
        val providerCollectionUid: String?,
        val imageUrl: String?,
        val name: String,
        val count: Int,
        val assetItems: List<AssetItem>
    )

    data class AssetItem(
        val nftUid: NftUid,
        val imageUrl: String?,
        val name: String,
        val count: Int,
        val onSale: Boolean,
        val price: NftPrice?,
        val priceInFiat: CurrencyValue? = null,
        val coinPrice: CoinPrice? = null
    )

}