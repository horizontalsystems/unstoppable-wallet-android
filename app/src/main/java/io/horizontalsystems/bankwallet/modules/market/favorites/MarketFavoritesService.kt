package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.category.MarketItemWrapper
import io.horizontalsystems.core.BackgroundManager
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class MarketFavoritesService(
    private val repository: MarketFavoritesRepository,
    private val menuService: MarketFavoritesMenuService,
    private val currencyManager: CurrencyManager,
    private val backgroundManager: BackgroundManager
) : BackgroundManager.Listener {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var favoritesJob: Job? = null

    private val marketItemsSubject: BehaviorSubject<DataState<List<MarketItemWrapper>>> =
        BehaviorSubject.create()
    val marketItemsObservable: Observable<DataState<List<MarketItemWrapper>>>
        get() = marketItemsSubject

    var sortDescending: Boolean = menuService.sortDescending
        set(value) {
            field = value
            menuService.sortDescending = value
            fetch()
        }

    var period: MarketFavoritesModule.Period = menuService.period
        set(value) {
            field = value
            menuService.period = value
            fetch()
        }

    private fun fetch() {
        favoritesJob?.cancel()
        favoritesJob = coroutineScope.launch {
            try {
                val marketItems = repository
                    .get(sortDescending, period, currencyManager.baseCurrency)
                    .map { MarketItemWrapper(it, true) }

                marketItemsSubject.onNext(DataState.Success(marketItems))
            } catch (e: Throwable) {
                marketItemsSubject.onNext(DataState.Error(e))
            }
        }
    }

    fun removeFavorite(uid: String) {
        repository.removeFavorite(uid)
    }

    fun refresh() {
        fetch()
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

        fetch()
    }

    fun stop() {
        backgroundManager.unregisterListener(this)
        coroutineScope.cancel()
    }

    override fun willEnterForeground() {
        fetch()
    }
}
