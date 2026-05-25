package io.horizontalsystems.bankwallet.modules.market.favorites

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.BackgroundManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.PriceManager
import io.horizontalsystems.bankwallet.core.managers.SignalsControlManager
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.widgets.MarketWidgetManager
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject

@HiltViewModel
class MarketFavoritesViewModel @Inject constructor(
    marketKit: MarketKitWrapper,
    marketFavoritesManager: MarketFavoritesManager,
    localStorage: ILocalStorage,
    marketWidgetManager: MarketWidgetManager,
    currencyManager: CurrencyManager,
    backgroundManager: BackgroundManager,
    priceManager: PriceManager,
) : ViewModelUiState<MarketFavoritesModule.UiState>() {
    private val service = MarketFavoritesService(
        MarketFavoritesRepository(marketKit, marketFavoritesManager),
        MarketFavoritesMenuService(localStorage, marketWidgetManager),
        currencyManager,
        backgroundManager,
        priceManager,
        SignalsControlManager(localStorage)
    )

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
