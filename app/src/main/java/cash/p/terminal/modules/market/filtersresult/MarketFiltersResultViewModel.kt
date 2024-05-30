package cash.p.terminal.modules.market.filtersresult

import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.ViewModelUiState
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.market.MarketDataValue
import cash.p.terminal.modules.market.MarketViewItem
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.category.MarketItemWrapper
import cash.p.terminal.ui.compose.Select
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class MarketFiltersResultViewModel(
    private val service: MarketFiltersResultService,
) : ViewModelUiState<MarketFiltersUiState>() {

    private var marketItems: List<MarketItemWrapper> = listOf()
    private var viewState: ViewState = ViewState.Loading
    private var viewItemsState: List<MarketViewItem> = listOf()

    init {
        viewModelScope.launch {
            service.stateObservable.asFlow().collect { state ->
                state.viewState?.let {
                    viewState = it
                    emitState()
                }

                state.dataOrNull?.let {
                    marketItems = it
                    syncMarketViewItems()
                    emitState()
                }
            }
        }

        service.start()
    }

    override fun createState() = MarketFiltersUiState(
        viewItems = viewItemsState,
        viewState = viewState,
        sortingField = service.sortingField,
        selectSortingField = Select(service.sortingField, service.sortingFields)
    )

    override fun onCleared() {
        service.stop()
    }

    fun onErrorClick() {
        service.refresh()
    }

    fun onSelectSortingField(sortingField: SortingField) {
        service.updateSortingField(sortingField)
        emitState()
    }

    fun onAddFavorite(uid: String) {
        service.addFavorite(uid)
    }

    fun onRemoveFavorite(uid: String) {
        service.removeFavorite(uid)
    }

    private fun syncMarketViewItems() {
        viewItemsState = marketItems.map { itemWrapper ->
            val marketCap = App.numberFormatter.formatFiatShort(
                itemWrapper.marketItem.marketCap.value,
                itemWrapper.marketItem.marketCap.currency.symbol,
                2
            )
            MarketViewItem(
                fullCoin = itemWrapper.marketItem.fullCoin,
                subtitle = marketCap,
                value = App.numberFormatter.formatFiatFull(
                    itemWrapper.marketItem.rate.value,
                    itemWrapper.marketItem.rate.currency.symbol
                ),
                marketDataValue = MarketDataValue.Diff(itemWrapper.marketItem.diff),
                rank = itemWrapper.marketItem.rank?.toString(),
                favorited = itemWrapper.favorited
            )
        }.toList()
    }

}

data class MarketFiltersUiState(
    val viewItems: List<MarketViewItem>,
    val viewState: ViewState,
    val sortingField: SortingField,
    val selectSortingField: Select<SortingField>
)