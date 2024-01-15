package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.category.MarketItemWrapper
import io.horizontalsystems.core.BackgroundManager
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class MarketFavoritesService(
    private val repository: MarketFavoritesRepository,
    private val menuService: MarketFavoritesMenuService,
    private val currencyManager: CurrencyManager,
    private val backgroundManager: BackgroundManager
) : BackgroundManager.Listener {
    private var favoritesDisposable: Disposable? = null
    private var repositoryDisposable: Disposable? = null
    private var currencyManagerDisposable: Disposable? = null

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
        favoritesDisposable?.dispose()

        repository.get(sortDescending, period, currencyManager.baseCurrency)
            .subscribeIO({ marketItems ->
                marketItemsSubject.onNext(DataState.Success(marketItems.map {
                    MarketItemWrapper(it, true)
                }))
            }, { error ->
                marketItemsSubject.onNext(DataState.Error(error))
            }).let {
                favoritesDisposable = it
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

        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO { fetch() }
            .let { currencyManagerDisposable = it }

        repository.dataUpdatedObservable
            .subscribeIO { fetch() }
            .let { repositoryDisposable = it }

        fetch()
    }

    fun stop() {
        backgroundManager.unregisterListener(this)
        favoritesDisposable?.dispose()
        currencyManagerDisposable?.dispose()
    }

    override fun willEnterForeground() {
        fetch()
    }
}
