package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.SortingField
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

    val sortingFieldTypes = SortingField.values().toList()
    var sortingField: SortingField = menuService.sortingField
        set(value) {
            field = value
            menuService.sortingField = value
            rebuildItems()
        }

    private fun fetch(forceRefresh: Boolean) {
        favoritesDisposable?.dispose()

        repository.get(sortingField, currencyManager.baseCurrency, forceRefresh)
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

    private fun rebuildItems() {
        fetch(false)
    }

    private fun forceRefresh() {
        fetch(true)
    }

    fun removeFavorite(uid: String) {
        repository.removeFavorite(uid)
    }

    fun refresh() {
        forceRefresh()
    }

    fun start() {
        backgroundManager.registerListener(this)

        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO { forceRefresh() }
            .let { currencyManagerDisposable = it }

        repository.dataUpdatedObservable
            .subscribeIO { forceRefresh() }
            .let { repositoryDisposable = it }

        forceRefresh()
    }

    fun stop() {
        backgroundManager.unregisterListener(this)
        favoritesDisposable?.dispose()
        currencyManagerDisposable?.dispose()
    }

    override fun willEnterForeground() {
        forceRefresh()
    }
}
