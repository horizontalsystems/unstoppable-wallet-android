package io.horizontalsystems.bankwallet.modules.market.filtersresult

import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.SignalsControlManager
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.category.MarketItemWrapper
import io.horizontalsystems.bankwallet.modules.market.filters.IMarketListFetcher
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.marketkit.models.Analytics
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
    private val signalsControlManager: SignalsControlManager,
    private val marketKitWrapper: MarketKitWrapper,
) {
    val showSignals: Boolean
        get() = signalsControlManager.showSignals
    val stateObservable: BehaviorSubject<DataState<List<MarketItemWrapper>>> =
        BehaviorSubject.create()

    var marketItems: List<MarketItem> = listOf()
    var signals: Map<String, Analytics.TechnicalAdvice.Advice> = mapOf()

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

    fun showSignals() {
        signalsControlManager.showSignals = true
        refresh()
    }

    fun hideSignals() {
        signalsControlManager.showSignals = false
        refresh()
    }

    private fun fetch() {
        fetchJob?.cancel()

        fetchJob = coroutineScope.launch {
            try {
                marketItems = fetcher.fetchAsync().await()
                if (showSignals) {
                    signals = marketKitWrapper
                        .getCoinSignalsSingle(marketItems.map { it.fullCoin.coin.uid })
                        .await()
                }
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
            .map {
                MarketItemWrapper(
                    marketItem = it,
                    favorited = favorites.contains(it.fullCoin.coin.uid),
                    signal = if (showSignals) signals[it.fullCoin.coin.uid] else null
                )
            }

        stateObservable.onNext(DataState.Success(items))
    }

}
