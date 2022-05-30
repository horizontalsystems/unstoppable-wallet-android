package io.horizontalsystems.bankwallet.modules.nft.collection.assets

import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.hsnft.HsNftApiV1Response
import io.horizontalsystems.bankwallet.modules.nft.INftApiProvider
import io.horizontalsystems.bankwallet.modules.nft.NftAssetRecord
import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem
import io.horizontalsystems.marketkit.models.CoinPrice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.rx2.asFlow
import java.util.concurrent.atomic.AtomicBoolean

class NftCollectionAssetsService(
    private val collectionUid: String,
    private val nftApiProvider: INftApiProvider,
    private val nftManager: NftManager,
    private val xRateRepository: BalanceXRateRepository
) {
    private val _items = MutableStateFlow<Result<List<CollectionAsset>>?>(null)
    val items = _items.filterNotNull()

    private var cursor: String? = null
    private val loading = AtomicBoolean(false)
    private val started = AtomicBoolean(false)
    private val coinUidsSet = mutableSetOf<String>()
    private var loadingJob: Job? = null

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

    suspend fun refresh() {
        _items.update { Result.success(listOf()) }

        loadingJob?.cancel()
        loading.set(false)

        cursor = null

        load(true)
    }

    private fun handleCoinPriceUpdate(xRatesMap: Map<String, CoinPrice?>) {
        _items.update { result ->
            result?.getOrNull()?.let {
                Result.success(
                    updateCurrencyValues(it, xRatesMap)
                )
            }
        }
    }

    private suspend fun load(initialLoad: Boolean = false) = withContext(Dispatchers.IO) {
        if (loading.getAndSet(true)) return@withContext

        loadingJob = launch {
            try {
                if (!initialLoad && cursor == null) {
                    _items.update { it }
                } else {
                    val (assets, cursor) = nftApiProvider.collectionAssets(collectionUid, cursor)

                    _items.update { handleAssets(assets, cursor) }
                }

                loading.set(false)
            } catch (cancellation: CancellationException) {
                //ignore
            } catch (error: Throwable) {
                _items.update { Result.failure(error) }
            }
        }
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

        val wholeList = (_items.value?.getOrNull() ?: listOf()) + assetItems

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
            collectionName = "",
            collectionLinks = null,
            averagePrice7d = null,
            averagePrice30d = null,
            totalSupply = 0
        ).let {
            CollectionAsset(it, it.stats.lastSale)
        }
}

data class CollectionAsset(
    val asset: NftAssetModuleAssetItem,
    val price: NftAssetModuleAssetItem.Price?
)
