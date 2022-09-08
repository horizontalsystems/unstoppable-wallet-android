package io.horizontalsystems.bankwallet.modules.nft.collection.events

import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.nft.NftPriceRecord
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.NftEvent
import io.horizontalsystems.marketkit.models.TokenQuery
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.rx2.asFlow
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class NftCollectionEventsService(
    private val eventListType: NftEventListType,
    var eventType: NftEvent.EventType,
    private val marketKit: MarketKitWrapper,
//    private val nftManager: NftManager,
    private val xRateRepository: BalanceXRateRepository
) {
    var items: Result<List<CollectionEvent>>? = null
    val itemsUpdatedFlow = MutableSharedFlow<Unit>()

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
            this.launch { handleCoinPriceUpdate(xRatesMap) }
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

    private suspend fun updateItems(items: Result<List<CollectionEvent>>?) {
        this.items = items
        itemsUpdatedFlow.emit(Unit)
    }

    private suspend fun restart() {
        updateItems(null)

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
                    updateItems(items)
                } else {
                    val type = if (eventType == NftEvent.EventType.All) null else eventType
                    val (events, cursor) = when (eventListType) {
                        is NftEventListType.Collection -> marketKit.nftCollectionEvents(eventListType.uid, type, cursor)
                        is NftEventListType.Asset -> marketKit.nftAssetEvents(eventListType.contractAddress, eventListType.tokenId, type, cursor)
                    }

                    updateItems(handleEvents(events, cursor))
                }

                loading.set(false)
            } catch (cancellation: CancellationException) {
                //ignore
            } catch (error: Throwable) {
                updateItems(Result.failure(error))
            }
        }
    }

    fun nftAssetPriceToCoinValue(nftPriceRecord: NftPriceRecord?): CoinValue? {
        if (nftPriceRecord == null) return null
        val tokenQuery = TokenQuery.fromId(nftPriceRecord.tokenQueryId) ?: return null
        val token = marketKit.token(tokenQuery) ?: return null

        return CoinValue(token, nftPriceRecord.value)
    }

    private fun handleEvents(
        events: List<NftEvent>,
        cursor: String?
    ): Result<List<CollectionEvent>> {
        this.cursor = cursor

//        val items = events.map { event ->
//            val assetItem = assetItem(event.asset)
//            val coinValue = nftAssetPriceToCoinValue(event.amount?.nftPriceRecord)
//            val amount = coinValue?.let { NftAssetModuleAssetItem.Price(it) }
//            CollectionEvent(event.type ?: NftEvent.EventType.All, event.date, assetItem, amount)
//        }
        val items = listOf<CollectionEvent>()

        val newCoinUids = items.mapNotNull { it.amount?.coinValue?.coin?.uid }

        coinUidsSet.addAll(newCoinUids)

        xRateRepository.setCoinUids(coinUidsSet.toList())

        val xRatesMap = xRateRepository.getLatestRates()

        val wholeList = (this.items?.getOrNull() ?: listOf()) + items

        return Result.success(
            updateCurrencyValues(wholeList, xRatesMap)
        )
    }

    private suspend fun handleCoinPriceUpdate(xRatesMap: Map<String, CoinPrice?>) {
        items?.getOrNull()?.let {
            updateItems(Result.success(updateCurrencyValues(it, xRatesMap)))
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

//    private fun assetItem(asset: NftAsset) =
//        nftManager.assetItem(
//            asset,
//            collectionName = "",
//            collectionLinks = null,
//            averagePrice7d = null,
//            averagePrice30d = null,
//            totalSupply = 0
//        )

}

data class CollectionEvent(
    val eventType: NftEvent.EventType,
    val date: Date?,
    val asset: NftAssetModuleAssetItem,
    val amount: NftAssetModuleAssetItem.Price?
)
