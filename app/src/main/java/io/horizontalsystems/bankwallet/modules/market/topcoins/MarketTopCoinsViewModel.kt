package io.horizontalsystems.bankwallet.modules.market.topcoins

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.modules.market.category.MarketItemWrapper
import io.horizontalsystems.bankwallet.modules.market.filters.TimePeriod
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

    fun onSelectPeriod(period: TimePeriod) {
        service.setPeriod(period)
    }

    fun onSelectSortingField(sortingField: SortingField) {
        service.setSortingField(sortingField)
    }

    fun onSelectTopMarket(topMarket: TopMarket) {
        service.setTopMarket(topMarket)
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
