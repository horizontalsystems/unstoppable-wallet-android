package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.PriceManager
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.category.MarketItemWrapper
import io.horizontalsystems.bankwallet.modules.market.filters.TimePeriod
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.marketkit.models.Analytics
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await

val TimeDuration.period: TimePeriod
    get() {
        return when (this) {
            TimeDuration.OneDay -> TimePeriod.TimePeriod_1D
            TimeDuration.SevenDay -> TimePeriod.TimePeriod_1W
            TimeDuration.ThirtyDay -> TimePeriod.TimePeriod_1M
            TimeDuration.ThreeMonths -> TimePeriod.TimePeriod_3M
        }
    }

class MarketFavoritesService(
    private val repository: MarketFavoritesRepository,
    private val menuService: MarketFavoritesMenuService,
    private val currencyManager: CurrencyManager,
    private val backgroundManager: BackgroundManager,
    private val priceManager: PriceManager
) : BackgroundManager.Listener {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var favoritesJob: Job? = null
    private var marketItems: List<MarketItem> = listOf()
    private var signals: Map<String, Analytics.TechnicalAdvice.Advice> = mapOf()

    private val marketItemsSubject: BehaviorSubject<DataState<List<MarketItemWrapper>>> =
        BehaviorSubject.create()
    val marketItemsObservable: Observable<DataState<List<MarketItemWrapper>>>
        get() = marketItemsSubject

    val showSignals by menuService::showSignals

    var watchlistSorting: WatchlistSorting = menuService.listSorting
        set(value) {
            field = value
            menuService.listSorting = value
            fetch()
        }

    var timeDuration: TimeDuration = menuService.timeDuration
        set(value) {
            field = value
            menuService.timeDuration = value
            fetch()
        }

    private fun fetch() {
        favoritesJob?.cancel()
        favoritesJob = coroutineScope.launch {
            try {
                marketItems = repository.get(timeDuration.period, currencyManager.baseCurrency)
                updateItems()
                if (menuService.showSignals) {
                    syncSignals()
                }
            } catch (e: CancellationException) {
                // no-op
            } catch (e: Throwable) {
                marketItemsSubject.onNext(DataState.Error(e))
            }
        }
    }

    private fun updateItems() {
        val sorting = watchlistSorting
        if (sorting == WatchlistSorting.Manual) {
            val manualSortOrder = menuService.manualSortOrder
            marketItems = marketItems.sortedBy {
                manualSortOrder.indexOf(it.fullCoin.coin.uid)
            }
        } else {
            val sortField = when (sorting) {
                WatchlistSorting.HighestCap -> SortingField.HighestCap
                WatchlistSorting.LowestCap -> SortingField.LowestCap
                WatchlistSorting.Gainers -> SortingField.TopGainers
                WatchlistSorting.Losers -> SortingField.TopLosers
                else -> throw IllegalStateException("Manual sorting should be handled separately")
            }
            marketItems = marketItems.sort(sortField)
        }
        val wrapperItems = marketItems.map {
            MarketItemWrapper(
                marketItem = it,
                favorited = true,
                signal = if (menuService.showSignals) signals[it.fullCoin.coin.uid] else null
            )
        }
        marketItemsSubject.onNext(DataState.Success(wrapperItems))
    }

    private fun syncSignals() {
        val uids = marketItems.map { it.fullCoin.coin.uid }
        coroutineScope.launch {
            try {
                signals = repository.getSignals(uids).await()
                updateItems()
            } catch (e: Throwable) {
                marketItemsSubject.onNext(DataState.Error(e))
            }
        }
    }

    override fun willEnterForeground() {
        fetch()
    }

    fun removeFavorite(uid: String) {
        repository.removeFavorite(uid)
    }

    fun refresh() {
        fetch()
        if (menuService.showSignals) {
            syncSignals()
        }
    }

    fun start() {
        backgroundManager.registerListener(this)

        coroutineScope.launch {
            currencyManager.baseCurrencyUpdatedSignal.asFlow().collect {
                fetch()
            }
        }

        coroutineScope.launch {
            repository.dataUpdatedObservable.asFlow().collect {
                fetch()
            }
        }

        coroutineScope.launch {
            currencyManager.baseCurrencyUpdatedFlow.collect {
                fetch()
            }
        }

        coroutineScope.launch {
            priceManager.priceChangeIntervalFlow.collect {
                fetch()
            }
        }

        fetch()
    }

    fun stop() {
        backgroundManager.unregisterListener(this)
        coroutineScope.cancel()
    }

    fun showSignals() {
        menuService.showSignals = true
        syncSignals()
    }

    fun hideSignals() {
        menuService.showSignals = false

        coroutineScope.launch {
            updateItems()
        }
    }

    fun reorder(from: Int, to: Int) {
        if (to < 0 || to >= marketItems.size) return
        coroutineScope.launch {
            val order = marketItems.map { it.fullCoin.coin.uid }
            menuService.manualSortOrder = order.toMutableList().apply {
                add(to, removeAt(from))
            }
            updateItems()
        }
    }
}
