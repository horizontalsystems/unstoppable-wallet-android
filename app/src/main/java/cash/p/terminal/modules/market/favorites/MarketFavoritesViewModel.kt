package cash.p.terminal.modules.market.favorites

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.core.ViewModelUiState


import cash.p.terminal.entities.DataState
import io.horizontalsystems.core.entities.ViewState
import cash.p.terminal.modules.market.MarketViewItem
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.modules.market.category.MarketItemWrapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class MarketFavoritesViewModel(
    private val service: MarketFavoritesService,
) : ViewModelUiState<MarketFavoritesModule.UiState>() {

    private var marketItemsWrapper: List<MarketItemWrapper> = listOf()
    val periods = listOf(
        TimeDuration.OneDay,
        TimeDuration.SevenDay,
        TimeDuration.ThirtyDay,
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
    private var showSignalsInfo = false

    init {
        viewModelScope.launch {
            service.marketItemsObservable.asFlow().collect { state ->
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
            showSignalsInfo = showSignalsInfo
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

    fun onToggleSignal() {
        if (service.showSignals) {
            service.hideSignals()
        } else {
            showSignalsInfo = true
            emitState()
        }
    }

    fun onSignalsInfoShown() {
        showSignalsInfo = false
        emitState()
    }

    fun showSignals() {
        service.showSignals()
    }

    fun reorder(from: Int, to: Int) {
        service.reorder(from, to)
    }
}
