package io.horizontalsystems.bankwallet.modules.nft.collection.events

import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem
import io.horizontalsystems.bankwallet.modules.nft.nftAssetPrice
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.NftAsset
import io.horizontalsystems.marketkit.models.NftEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.rx2.asFlow
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class NftCollectionEventsService(
        private val eventListType: NftEventListType,
        var eventType: NftEvent.EventType,
        private val marketKit: MarketKit,
        private val nftManager: NftManager,
        private val xRateRepository: BalanceXRateRepository
) {
    private val _items = MutableStateFlow<Result<List<CollectionEvent>>?>(null)
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

    suspend fun loadMore() {
        load()
    }

    suspend fun setEventType(eventType: NftEvent.EventType) {
        if (this.eventType == eventType) return
        this.eventType = eventType

        restart()
    }

    suspend fun refresh() {
       restart()
    }

    private suspend fun restart() {
        _items.update { Result.success(listOf()) }

        loadingJob?.cancel()
        loading.set(false)

        cursor = null

        load(true)
    }


    private suspend fun load(initialLoad: Boolean = false) = withContext(Dispatchers.IO) {
        if (loading.getAndSet(true)) return@withContext

        loadingJob = launch {
            try {
                if (!initialLoad && cursor == null) {
                    _items.update { it }
                } else {
                    val type = if (eventType == NftEvent.EventType.All) null else eventType
                    val (events, cursor) = when (eventListType) {
                        is NftEventListType.Collection -> marketKit.nftCollectionEvents(eventListType.uid, type, cursor)
                        is NftEventListType.Asset -> marketKit.nftAssetEvents(eventListType.contractAddress, eventListType.tokenId, type, cursor)
                    }


                    _items.update { handleEvents(events, cursor) }
                }

                loading.set(false)
            } catch (cancellation: CancellationException) {
                //ignore
            } catch (error: Throwable) {
                _items.update { Result.failure(error) }
            }
        }
    }

    private fun handleEvents(
        events: List<NftEvent>,
        cursor: String?
    ): Result<List<CollectionEvent>> {
        this.cursor = cursor

        val items = events.map { event ->
            val assetItem = assetItem(event.asset)
            val coinValue = nftManager.nftAssetPriceToCoinValue(event.amount?.nftAssetPrice)
            val amount = coinValue?.let { NftAssetModuleAssetItem.Price(it) }
            CollectionEvent(event.type ?: NftEvent.EventType.All, event.date, assetItem, amount)
        }
        val newCoinUids = items.mapNotNull { it.amount?.coinValue?.coin?.uid }

        coinUidsSet.addAll(newCoinUids)

        xRateRepository.setCoinUids(coinUidsSet.toList())

        val xRatesMap = xRateRepository.getLatestRates()

        val wholeList = (_items.value?.getOrNull() ?: listOf()) + items

        return Result.success(
            updateCurrencyValues(wholeList, xRatesMap)
        )
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

    private fun updateCurrencyValues(
        events: List<CollectionEvent>,
        xRatesMap: Map<String, CoinPrice?>
    ) = events.map { asset ->
        val coinValue = asset.amount?.coinValue ?: return@map asset
        val coinPrice = xRatesMap[coinValue.coin.uid] ?: return@map asset

        val currencyValue = CurrencyValue(baseCurrency, coinValue.value.times(coinPrice.value))

        asset.copy(amount = NftAssetModuleAssetItem.Price(coinValue, currencyValue))
    }

    private fun assetItem(asset: NftAsset) =
        nftManager.assetItem(
            asset,
            collectionName = "",
            collectionLinks = null,
            averagePrice7d = null,
            averagePrice30d = null,
            totalSupply = 0
        )

}

data class CollectionEvent(
    val eventType: NftEvent.EventType,
    val date: Date?,
    val asset: NftAssetModuleAssetItem,
    val amount: NftAssetModuleAssetItem.Price?
)
