package io.horizontalsystems.bankwallet.modules.nft.collection.items

import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.hsnft.HsNftApiV1Response
import io.horizontalsystems.bankwallet.modules.nft.INftApiProvider
import io.horizontalsystems.bankwallet.modules.nft.NftAssetRecord
import io.horizontalsystems.bankwallet.modules.nft.NftCollection
import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem
import io.horizontalsystems.marketkit.models.CoinPrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class NftCollectionAssetsService(
    private val collection: NftCollection,
    private val nftApiProvider: INftApiProvider,
    private val nftManager: NftManager,
    private val xRateRepository: BalanceXRateRepository
) {
    private val _nftCollectionAssets = MutableStateFlow<Result<List<CollectionAsset>>?>(null)
    val nftCollectionAssets = _nftCollectionAssets.filterNotNull()

    private var cursor: String? = null
    private val loading = AtomicBoolean(false)
    private val started = AtomicBoolean(false)
    private val coinUidsSet = mutableSetOf<String>()

    private val baseCurrency by xRateRepository::baseCurrency

    suspend fun start() = withContext(Dispatchers.IO) {
        if (started.getAndSet(true)) return@withContext

        load(true)

        xRateRepository.itemObservable.asFlow().collectWith(this) { xRatesMap ->
            handleCoinPriceUpdate(xRatesMap)
        }
    }

    suspend fun loadMore() = withContext(Dispatchers.IO) {
        load()
    }

    private fun handleCoinPriceUpdate(xRatesMap: Map<String, CoinPrice?>) {
        _nftCollectionAssets.update { result ->
            result?.getOrNull()?.let {
                Result.success(
                    updateCurrencyValues(it, xRatesMap)
                )
            }
        }
    }

    private suspend fun load(initialLoad: Boolean = false) {
        if (loading.getAndSet(true)) return

        if (!initialLoad && cursor == null) {
            _nftCollectionAssets.update { it }
        } else {
            try {
                val (assets, cursor) = nftApiProvider.collectionAssets(collection.uid, cursor)

                _nftCollectionAssets.update { handleAssets(assets, cursor) }
            } catch (error: Throwable) {
                _nftCollectionAssets.update { Result.failure(error) }
            }
        }

        loading.set(false)
    }

    private fun handleAssets(
        assets: List<NftAssetRecord>,
        cursor: HsNftApiV1Response.Cursor
    ): Result<List<CollectionAsset>> {
        this.cursor = cursor.next

        val assetItems = assets.map { asset -> collectionAsset(asset) }
        val newCoinUids = assetItems.mapNotNull { it.price?.coinValue?.coin?.uid }

        coinUidsSet.addAll(newCoinUids)

        xRateRepository.setCoinUids(coinUidsSet.toList())

        val xRatesMap = xRateRepository.getLatestRates()

        val wholeList = (_nftCollectionAssets.value?.getOrNull() ?: listOf()) + assets.map { collectionAsset(it) }

        return Result.success(updateCurrencyValues(wholeList, xRatesMap))
    }

    private fun updateCurrencyValues(
        assets: List<CollectionAsset>,
        xRatesMap: Map<String, CoinPrice?>
    ) = assets.map { asset ->
        val coinValue = asset.price?.coinValue ?: return@map asset
        val coinPrice = xRatesMap[coinValue.coin.uid] ?: return@map asset

        val currencyValue = CurrencyValue(baseCurrency, coinValue.value.times(coinPrice.value))

        asset.copy(price = NftAssetModuleAssetItem.Price(coinValue, currencyValue))
    }

    private fun collectionAsset(assetRecord: NftAssetRecord) =
        nftManager.assetItem(
            assetRecord = assetRecord,
            collectionName = collection.name,
            collectionLinks = collection.links?.let {
                HsNftApiV1Response.Collection.Links(
                    it.externalUrl,
                    it.discordUrl,
                    it.telegramUrl,
                    it.twitterUsername,
                    it.instagramUsername,
                    it.wikiUrl
                )
            },
            averagePrice7d = null,
            averagePrice30d = null,
            totalSupply = collection.totalSupply
        ).let {
            CollectionAsset(it, it.stats.lastSale)
        }
}

data class CollectionAsset(
    val asset: NftAssetModuleAssetItem,
    val price: NftAssetModuleAssetItem.Price?
)
