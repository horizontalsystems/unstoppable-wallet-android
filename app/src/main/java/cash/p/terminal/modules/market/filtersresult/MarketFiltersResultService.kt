package cash.p.terminal.modules.market.filtersresult

import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.market.MarketItem
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.category.MarketItemWrapper
import cash.p.terminal.modules.market.filters.IMarketListFetcher
import cash.p.terminal.modules.market.sort
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await

class MarketFiltersResultService(
    private val fetcher: IMarketListFetcher,
    private val favoritesManager: MarketFavoritesManager,
) {
    val stateObservable: BehaviorSubject<DataState<List<MarketItemWrapper>>> =
        BehaviorSubject.create()

    var marketItems: List<MarketItem> = listOf()

    val sortingFields = listOf(
        SortingField.HighestCap,
        SortingField.LowestCap,
        SortingField.TopGainers,
        SortingField.TopLosers,
    )

    var sortingField = SortingField.HighestCap

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var fetchJob: Job? = null

    fun start() {
        coroutineScope.launch {
            favoritesManager.dataUpdatedAsync.asFlow().collect {
                syncItems()
            }
        }

        fetch()
    }

    fun stop() {
        coroutineScope.cancel()
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
        fetchJob?.cancel()

        fetchJob = coroutineScope.launch {
            try {
                marketItems = fetcher.fetchAsync().await()
                syncItems()
            } catch (e: Throwable) {
                stateObservable.onNext(DataState.Error(e))
            }
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
