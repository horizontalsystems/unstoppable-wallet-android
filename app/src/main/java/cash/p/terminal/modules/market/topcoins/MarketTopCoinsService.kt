package cash.p.terminal.modules.market.topcoins

import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.market.MarketField
import cash.p.terminal.modules.market.MarketItem
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.TopMarket
import cash.p.terminal.modules.market.category.MarketItemWrapper
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await

class MarketTopCoinsService(
    private val marketTopMoversRepository: MarketTopMoversRepository,
    private val currencyManager: CurrencyManager,
    private val favoritesManager: MarketFavoritesManager,
    topMarket: TopMarket = TopMarket.Top100,
    sortingField: SortingField = SortingField.HighestCap,
    private val marketField: MarketField,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var syncJob: Job? = null

    private var marketItems: List<MarketItem> = listOf()

    val stateObservable: BehaviorSubject<DataState<List<MarketItemWrapper>>> =
        BehaviorSubject.create()

    val topMarkets = TopMarket.values().toList()
    var topMarket: TopMarket = topMarket
        private set

    val sortingFields = SortingField.values().toList()
    var sortingField: SortingField = sortingField
        private set

    fun setSortingField(sortingField: SortingField) {
        this.sortingField = sortingField
        sync()
    }

    fun setTopMarket(topMarket: TopMarket) {
        this.topMarket = topMarket
        sync()
    }

    private fun sync() {
        syncJob?.cancel()
        syncJob = coroutineScope.launch {
            try {
                marketItems = marketTopMoversRepository.get(
                    topMarket.value,
                    sortingField,
                    topMarket.value,
                    currencyManager.baseCurrency
                ).await()

                syncItems()
            } catch (e: Throwable) {
                stateObservable.onNext(DataState.Error(e))
            }
        }
    }

    private fun syncItems() {
        val favorites = favoritesManager.getAll().map { it.coinUid }
        val items =
            marketItems.map { MarketItemWrapper(it, favorites.contains(it.fullCoin.coin.uid)) }
        stateObservable.onNext(DataState.Success(items))
    }

    fun start() {
        coroutineScope.launch {
            favoritesManager.dataUpdatedAsync.asFlow().collect {
                syncItems()
            }
        }

        sync()
    }

    fun refresh() {
        sync()
    }

    fun stop() {
        coroutineScope.cancel()
    }

    fun addFavorite(coinUid: String) {
        favoritesManager.add(coinUid)
    }

    fun removeFavorite(coinUid: String) {
        favoritesManager.remove(coinUid)
    }
}
