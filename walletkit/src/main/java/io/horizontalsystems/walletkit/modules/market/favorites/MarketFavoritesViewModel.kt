package io.horizontalsystems.walletkit.modules.market.favorites

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.StatSection
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.entities.DataState
import io.horizontalsystems.walletkit.entities.ViewState
import io.horizontalsystems.walletkit.modules.market.MarketViewItem
import io.horizontalsystems.walletkit.modules.market.TimeDuration
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MarketFavoritesViewModel(
    private val service: MarketFavoritesService,
) : ViewModelUiState<MarketFavoritesModule.UiState>() {

    private var marketItemsWrapper: List<MarketItemWrapper> = listOf()
    val periods = listOf(
        TimeDuration.OneDay,
        TimeDuration.SevenDay,
        TimeDuration.ThirtyDay,
        TimeDuration.ThreeMonths,
    )

    val sortingOptions = listOf(
        WatchlistSorting.Manual,
        WatchlistSorting.HighestCap,
        WatchlistSorting.LowestCap,
        WatchlistSorting.Gainers,
        WatchlistSorting.Losers,
    )

    private var isRefreshing = false
    private var viewState: ViewState = ViewState.Loading

    init {
        viewModelScope.launch {
            service.marketItemsFlow.collect { state ->
                when (state) {
                    is DataState.Success -> {
                        viewState = ViewState.Success
                        marketItemsWrapper = state.data
                    }

                    is DataState.Error -> {
                        viewState = ViewState.Error(state.error)
                    }

                    DataState.Loading -> {}
                }
                emitState()
            }
        }

        viewModelScope.launch {
            UserSubscriptionManager.activeSubscriptionStateFlow.collect {
                refresh()
            }
        }

        service.start()
    }

    override fun createState(): MarketFavoritesModule.UiState {
        return MarketFavoritesModule.UiState(
            viewItems = marketItemsWrapper.map {
                MarketViewItem.create(it.marketItem, favorited = true, advice = it.signal)
            },
            viewState = viewState,
            isRefreshing = isRefreshing,
            sortingField = service.watchlistSorting,
            period = service.timeDuration,
            showSignal = service.showSignals,
        )
    }

    private fun refreshWithMinLoadingSpinnerPeriod() {
        isRefreshing = true
        emitState()
        service.refresh()
        viewModelScope.launch {
            delay(1000)
            isRefreshing = false
            emitState()
        }
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onSelectPeriod(period: TimeDuration) {
        service.timeDuration = period
    }

    override fun onCleared() {
        service.stop()
    }

    fun removeFromFavorites(uid: String) {
        service.removeFavorite(uid)
    }

    fun onSelectSortingField(sortingField: WatchlistSorting) {
        service.watchlistSorting = sortingField
        emitState()
    }

    fun hideSignals() {
        service.hideSignals()

        stat(
            page = StatPage.Markets,
            event = StatEvent.ShowSignals(false),
            section = StatSection.Watchlist
        )
    }

    fun showSignals() {
        service.showSignals()

        stat(
            page = StatPage.Markets,
            event = StatEvent.ShowSignals(true),
            section = StatSection.Watchlist
        )
    }

    fun reorder(from: Int, to: Int) {
        service.reorder(from, to)
    }
}
