package cash.p.terminal.modules.market.topcoins

import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.ViewModelUiState
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.market.MarketField
import cash.p.terminal.modules.market.MarketViewItem
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.modules.market.TopMarket
import cash.p.terminal.modules.market.category.MarketItemWrapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class MarketTopCoinsViewModel(
    private val service: MarketTopCoinsService,
    private var marketField: MarketField,
) : ViewModelUiState<MarketTopCoinsModule.UiState>() {

    private var marketItems: List<MarketItemWrapper> = listOf()

    val periods by service::periods
    val topMarkets by service::topMarkets
    val sortingFields by service::sortingFields
    private var viewItems = emptyList<MarketViewItem>()
    private var viewState: ViewState = ViewState.Loading
    private var isRefreshing = false

    init {
        viewModelScope.launch {
            service.stateObservable.asFlow().collect {
                syncState(it)
            }
        }

        service.start()
    }

    override fun createState(): MarketTopCoinsModule.UiState {
        return MarketTopCoinsModule.UiState(
            viewItems,
            viewState,
            isRefreshing,
            service.sortingField,
            service.topMarket,
            service.period
        )
    }

    private fun syncState(state: DataState<List<MarketItemWrapper>>) {
        state.viewState?.let {
            viewState = it
        }

        state.dataOrNull?.let {
            marketItems = it

            syncMarketViewItems()
        }
    }

    private fun syncMarketViewItems() {
        viewItems = marketItems.map {
            MarketViewItem.create(it.marketItem, marketField, it.favorited)
        }
        emitState()
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

    fun onSelectPeriod(timeDuration: TimeDuration) {
        service.setTimeDuration(timeDuration)
        emitState()
    }

    fun onSelectSortingField(sortingField: SortingField) {
        service.setSortingField(sortingField)
        emitState()
    }

    fun onSelectTopMarket(topMarket: TopMarket) {
        service.setTopMarket(topMarket)
        emitState()
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    override fun onCleared() {
        service.stop()
    }

    fun onAddFavorite(coinUid: String) {
        service.addFavorite(coinUid)
    }

    fun onRemoveFavorite(coinUid: String) {
        service.removeFavorite(coinUid)
    }
}
