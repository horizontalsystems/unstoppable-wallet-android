package io.horizontalsystems.bankwallet.modules.market.topcoins

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.modules.market.category.MarketItemWrapper
import io.horizontalsystems.bankwallet.modules.market.filters.TimePeriod
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
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var syncJob: Job? = null

    private var marketItems: List<MarketItem> = listOf()

    val stateObservable: BehaviorSubject<DataState<List<MarketItemWrapper>>> =
        BehaviorSubject.create()

    val periods = listOf(
        TimePeriod.TimePeriod_1D,
        TimePeriod.TimePeriod_1W,
        TimePeriod.TimePeriod_1M,
        TimePeriod.TimePeriod_3M,
    )
    var period: TimePeriod = periods[0]
        private set

    val topMarkets = TopMarket.entries
    var topMarket: TopMarket = topMarket
        private set

    val sortingFields = SortingField.entries
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

    fun setPeriod(period: TimePeriod) {
        this.period = period
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
                    currencyManager.baseCurrency,
                    period,
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
