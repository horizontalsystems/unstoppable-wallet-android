package io.horizontalsystems.bankwallet.modules.market.filtersresult

import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.category.MarketCategoryModule
import io.horizontalsystems.bankwallet.modules.market.category.MarketItemWrapper
import io.horizontalsystems.bankwallet.modules.market.filters.IMarketListFetcher
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class MarketFiltersResultService(
    private val fetcher: IMarketListFetcher,
    private val favoritesManager: MarketFavoritesManager,
) {
    val stateObservable: BehaviorSubject<DataState<List<MarketItemWrapper>>> =
        BehaviorSubject.create()

    var marketItems: List<MarketItem> = listOf()

    val sortingFields = SortingField.values().toList()
    private val marketFields = MarketField.values().toList()
    var sortingField = SortingField.HighestCap
    var marketField = MarketField.PriceDiff

    val menu: MarketCategoryModule.Menu
        get() = MarketCategoryModule.Menu(
            Select(sortingField, sortingFields),
            Select(marketField, marketFields)
        )

    private var fetchDisposable: Disposable? = null
    private var favoriteDisposable: Disposable? = null

    fun start() {
        fetch()

        favoritesManager.dataUpdatedAsync
            .subscribeIO {
                syncItems()
            }.let {
                favoriteDisposable = it
            }
    }

    fun stop() {
        favoriteDisposable?.dispose()
        fetchDisposable?.dispose()
    }

    fun refresh() {
        fetch()
    }

    fun updateSortingField(sortingField: SortingField) {
        this.sortingField = sortingField
        syncItems()
    }

    fun addFavorite(coinUid: String) {
        favoritesManager.add(coinUid)
    }

    fun removeFavorite(coinUid: String) {
        favoritesManager.remove(coinUid)
    }

    private fun fetch() {
        fetchDisposable?.dispose()

        fetcher.fetchAsync()
            .subscribeIO({
                marketItems = it
                syncItems()
            }, {
                stateObservable.onNext(DataState.Error(it))
            }).let {
                fetchDisposable = it
            }
    }

    private fun syncItems() {
        val favorites = favoritesManager.getAll().map { it.coinUid }

        val items = marketItems
            .sort(sortingField)
            .map { MarketItemWrapper(it, favorites.contains(it.fullCoin.coin.uid)) }

        stateObservable.onNext(DataState.Success(items))
    }

}
