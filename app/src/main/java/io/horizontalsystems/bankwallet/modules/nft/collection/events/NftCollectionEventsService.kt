package io.horizontalsystems.bankwallet.modules.nft.collection.events

import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.providers.nft.NftEventsProvider
import io.horizontalsystems.bankwallet.core.providers.nft.PaginationData
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.nft.NftEventMetadata
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.coin.ContractInfo
import io.horizontalsystems.bankwallet.modules.market.overview.coinValue
import io.horizontalsystems.marketkit.models.CoinPrice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.rx2.asFlow
import java.util.concurrent.atomic.AtomicBoolean

class NftCollectionEventsService(
    private val eventListType: NftEventListType,
    eventType: NftEventMetadata.EventType,
    private val nftEventsProvider: NftEventsProvider,
    private val xRateRepository: BalanceXRateRepository
) {
    var items: Result<List<Item>>? = null
    val itemsUpdatedFlow = MutableSharedFlow<Unit>()
    var eventType: NftEventMetadata.EventType = eventType
        private set
    val contracts: List<ContractInfo> = (eventListType as? NftEventListType.Collection)?.contracts ?: listOf()
    var contract: ContractInfo? = contracts.firstOrNull()
        private set
    private var paginationData: PaginationData? = null
    private val loading = AtomicBoolean(false)
    private val started = AtomicBoolean(false)
    private val coinUidsSet = mutableSetOf<String>()
    private var loadingJob: Job? = null

    private val baseCurrency by xRateRepository::baseCurrency

    suspend fun start() = withContext(Dispatchers.IO) {
        if (started.getAndSet(true)) return@withContext

        load(true)

        xRateRepository.itemObservable.asFlow().collectWith(this) { latestRates ->
            this.launch { handleCoinPriceUpdate(latestRates) }
        }
    }

    suspend fun loadMore() {
        load()
    }

    suspend fun setEventType(eventType: NftEventMetadata.EventType) {
        if (this.eventType == eventType) return
        this.eventType = eventType

        restart()
    }

    suspend fun setContract(contract: ContractInfo) {
        if (this.contract == contract) return
        this.contract = contract

        restart()
    }

    suspend fun refresh() {
        restart()
    }

    private suspend fun updateItems(items: Result<List<Item>>?) {
        this.items = items
        itemsUpdatedFlow.emit(Unit)
    }

    private suspend fun restart() {
        updateItems(null)

        loadingJob?.cancel()
        loading.set(false)

        paginationData = null

        load(true)
    }


    private suspend fun load(initialLoad: Boolean = false) = withContext(Dispatchers.IO) {
        if (loading.getAndSet(true)) return@withContext

        loadingJob = launch {
            try {
                if (!initialLoad && paginationData == null) {
                    updateItems(items)
                } else {
                    val (events, cursor) = when (eventListType) {
                        is NftEventListType.Collection -> {
                            nftEventsProvider.collectionEventsMetadata(
                                eventListType.blockchainType,
                                eventListType.providerUid,
                                contract?.rawValue ?: "",
                                eventType,
                                paginationData
                            )
                        }
                        is NftEventListType.Asset -> {
                            nftEventsProvider.assetEventsMetadata(eventListType.nftUid, eventType, paginationData)
                        }
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

    private fun handleEvents(
        events: List<NftEventMetadata>,
        paginationData: PaginationData?
    ): Result<List<Item>> {
        this.paginationData = paginationData

        val items = events.map { Item(it) }
        val newCoinUids = items.mapNotNull { it.event.amount?.token?.coin?.uid }

        coinUidsSet.addAll(newCoinUids)

        xRateRepository.setCoinUids(coinUidsSet.toList())

        val latestRates = xRateRepository.getLatestRates()

        val wholeList = (this.items?.getOrNull() ?: listOf()) + items

        return Result.success(updateCurrencyValues(wholeList, latestRates))
    }

    private suspend fun handleCoinPriceUpdate(latestRates: Map<String, CoinPrice?>) {
        items?.getOrNull()?.let {
            updateItems(Result.success(updateCurrencyValues(it, latestRates)))
        }
    }

    private fun updateCurrencyValues(
        events: List<Item>,
        latestRates: Map<String, CoinPrice?>
    ) = events.map { item ->
        val coinValue = item.event.amount?.coinValue ?: return@map item
        val coinPrice = latestRates[coinValue.coin.uid] ?: return@map item
        item.copy(priceInFiat = CurrencyValue(baseCurrency, coinValue.value.times(coinPrice.value)))
    }

    data class Item(
        val event: NftEventMetadata,
        val priceInFiat: CurrencyValue? = null
    )
}

