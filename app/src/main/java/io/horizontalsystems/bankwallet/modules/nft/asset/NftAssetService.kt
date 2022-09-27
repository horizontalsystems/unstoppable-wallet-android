package io.horizontalsystems.bankwallet.modules.nft.asset

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.adapters.nft.INftAdapter
import io.horizontalsystems.bankwallet.core.managers.NftAdapterManager
import io.horizontalsystems.bankwallet.core.providers.nft.INftProvider
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.nft.NftAssetMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftCollectionMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftKey
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.NftPrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.collect
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.*

class NftAssetService(
    private val providerCollectionUid: String,
    val nftUid: NftUid,
    private val accountManager: IAccountManager,
    private val nftAdapterManager: NftAdapterManager,
    private val provider: INftProvider,
    private val xRateRepository: BalanceXRateRepository
) {
    private val _serviceDataFlow = MutableStateFlow<Result<Item>?>(null)
    private var adapter: INftAdapter? = null
    private var isOwned = false
    val serviceDataFlow = _serviceDataFlow.filterNotNull()

    val providerTitle = provider.title
    val providerIcon = provider.icon

    suspend fun start() = withContext(Dispatchers.IO) {
        launch {
            xRateRepository.itemObservable.collect {
                handleXRateUpdate(it)
            }
        }
        loadAsset()
        accountManager.activeAccount?.let { account ->
            if (!account.isWatchAccount) {
                val nftKey = NftKey(account, nftUid.blockchainType)
                nftAdapterManager.adapter(nftKey)?.let { nftAdapter ->
                    adapter = nftAdapter
                    launch {
                        nftAdapter.nftRecordsFlow.collect {
                            handleUpdated()
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleUpdated() {
        val currentItem = _serviceDataFlow.value?.getOrNull() ?: return
        isOwned = isOwned(currentItem.asset.nftUid)
        loadAsset()
    }

    private fun handleXRateUpdate(latestRates: Map<String, CoinPrice?>) {
        val currentItem = _serviceDataFlow.value?.getOrNull() ?: return
        _serviceDataFlow.update {
            Result.success(currentItem.updateRates(latestRates))
        }
    }

    private fun isOwned(nftUid: NftUid): Boolean {
        return adapter?.nftRecord(nftUid) != null
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        loadAsset()
    }

    private suspend fun loadAsset() {
        try {
            val (assetMetadata, collectionMetadata) = provider.extendedAssetMetadata(nftUid, providerCollectionUid)

            handle(item(assetMetadata, collectionMetadata))
        } catch (error: Exception) {
            _serviceDataFlow.tryEmit(
                Result.failure(error)
            )
        }
    }

    private fun item(asset: NftAssetMetadata, collection: NftCollectionMetadata): Item {
        return Item(
            asset = asset,
            collection = collection,
            lastSale = PriceItem(asset.lastSalePrice),
            average7d = PriceItem(collection.stats7d?.averagePrice),
            average30d = PriceItem(collection.stats30d?.averagePrice),
            collectionFloor = PriceItem(collection.floorPrice),
            offers = asset.offers.map { PriceItem(it) },
            sale = asset.saleInfo?.let { saleInfo ->
                SaleItem(
                    saleInfo.type,
                    saleInfo.listings.map { listing -> SaleListingItem(listing.untilDate, PriceItem(listing.price)) })
            },
            owned = isOwned
        )
    }

    private fun allCoinUids(item: Item): List<String> {
        val priceItems = mutableListOf(item.lastSale, item.average7d, item.average30d, item.collectionFloor)

        priceItems.addAll(item.offers)

        item.sale?.let { saleItem ->
            priceItems.addAll(saleItem.listings.map { it.price })
        }

        return priceItems.mapNotNull { it?.price?.token?.coin?.uid }.distinct()
    }

    private fun handle(item: Item) {
        val coinUids = allCoinUids(item)
        xRateRepository.setCoinUids(coinUids)

        val latestRates = xRateRepository.getLatestRates()
        _serviceDataFlow.tryEmit(Result.success(item.updateRates(latestRates)))
    }

    private fun Item.updateRates(latestRates: Map<String, CoinPrice?>): Item = copy(
        lastSale = lastSale?.setCurrencyValue(latestRates),
        average7d = average7d?.setCurrencyValue(latestRates),
        average30d = average30d?.setCurrencyValue(latestRates),
        collectionFloor = collectionFloor?.setCurrencyValue(latestRates),
        offers = offers.map { it.setCurrencyValue(latestRates) },
        sale = sale?.let { saleItem ->
            saleItem.copy(listings = saleItem.listings.map { saleListingItem ->
                saleListingItem.copy(
                    price = saleListingItem.price.setCurrencyValue(latestRates)
                )
            })
        }
    )

    private fun PriceItem.setCurrencyValue(latestRates: Map<String, CoinPrice?>): PriceItem {
        return if (price == null) this
        else {
            copy(priceInFiat = latestRates[price.token.coin.uid]?.let { latestRate ->
                CurrencyValue(xRateRepository.baseCurrency, price.value.multiply(latestRate.value))
            })
        }
    }

    data class Item(
        val asset: NftAssetMetadata,
        val collection: NftCollectionMetadata,

        val lastSale: PriceItem?,
        val average7d: PriceItem?,
        val average30d: PriceItem?,
        val collectionFloor: PriceItem?,
        val offers: List<PriceItem>,
        val sale: SaleItem?,
        val owned: Boolean
    ) {
        val bestOffer: PriceItem?
            get() {
                if (offers.isEmpty() || offers.any { it.priceInFiat == null }) return null

                val sortedOffers = offers.sortedByDescending { it.priceInFiat?.value ?: BigDecimal.ZERO }
                return sortedOffers.first()
            }
    }

    data class PriceItem(
        val price: NftPrice?,
        val priceInFiat: CurrencyValue? = null
    )

    data class SaleItem(
        val type: NftAssetMetadata.SaleType,
        val listings: List<SaleListingItem>
    ) {
        val bestListing: SaleListingItem?
            get() {
                if (listings.isEmpty() || listings.any { it.price.priceInFiat == null }) return null

                val sortedListings = listings.sortedBy { it.price.priceInFiat?.value ?: BigDecimal.ZERO }
                return sortedListings.first()
            }
    }

    data class SaleListingItem(
        val untilDate: Date,
        val price: PriceItem
    )
}
