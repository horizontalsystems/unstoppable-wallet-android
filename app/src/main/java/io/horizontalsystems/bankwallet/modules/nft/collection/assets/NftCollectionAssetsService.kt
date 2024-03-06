package io.horizontalsystems.bankwallet.modules.nft.collection.assets

import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.providers.nft.INftProvider
import io.horizontalsystems.bankwallet.core.providers.nft.PaginationData
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.nft.NftAssetMetadata
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.market.overview.coinValue
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.NftPrice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.rx2.asFlow
import java.util.concurrent.atomic.AtomicBoolean

class NftCollectionAssetsService(
    private val blockchainType: BlockchainType,
    private val collectionUid: String,
    private val provider: INftProvider,
    private val xRateRepository: BalanceXRateRepository
) {
    private val _items = MutableStateFlow<Result<List<Item>>?>(null)
    val items = _items.filterNotNull()

    private var paginationData: PaginationData? = null
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

        paginationData = null

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
                if (!initialLoad && paginationData == null) {
                    _items.update { it }
                } else {
//                    val (assets, paginationData) = provider.collectionAssetsMetadata(blockchainType, collectionUid, paginationData)
//                    _items.update { handle(assets, paginationData) }
                }

                loading.set(false)
            } catch (cancellation: CancellationException) {
                //ignore
            } catch (error: Throwable) {
                _items.update { Result.failure(error) }
            }
        }
    }

    private fun handle(
        assets: List<NftAssetMetadata>,
        paginationData: PaginationData?
    ): Result<List<Item>> {
        this.paginationData = paginationData

        val assetItems = assets.map { asset -> Item(asset, asset.lastSalePrice) }
        val newCoinUids = assetItems.mapNotNull { it.price?.token?.coin?.uid }

        coinUidsSet.addAll(newCoinUids)

        xRateRepository.setCoinUids(coinUidsSet.toList())

        val latestRates = xRateRepository.getLatestRates()

        val wholeList = (_items.value?.getOrNull() ?: listOf()) + assetItems

        return Result.success(updateCurrencyValues(wholeList, latestRates))
    }

    private fun updateCurrencyValues(
        assets: List<Item>,
        latestRates: Map<String, CoinPrice?>
    ) = assets.map { asset ->
        val coinValue = asset.price?.coinValue ?: return@map asset
        val coinPrice = latestRates[coinValue.coin.uid] ?: return@map asset

        val currencyValue = CurrencyValue(baseCurrency, coinValue.value.times(coinPrice.value))
        asset.copy(priceInFiat = currencyValue)
    }

    data class Item(
        val asset: NftAssetMetadata,
        val price: NftPrice?,
        val priceInFiat: CurrencyValue? = null
    )
}
