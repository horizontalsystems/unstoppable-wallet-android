package cash.p.terminal.modules.market.filtersresult

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.core.stats.statField
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.market.MarketField
import cash.p.terminal.modules.market.MarketViewItem
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.category.MarketItemWrapper
import cash.p.terminal.modules.market.topcoins.SelectorDialogState
import cash.p.terminal.ui.compose.Select
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class MarketFiltersResultViewModel(
    private val service: MarketFiltersResultService,
) : ViewModel() {

    private var marketItems: List<MarketItemWrapper> = listOf()

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    var viewItemsState by mutableStateOf<List<MarketViewItem>>(listOf())
        private set

    var selectorDialogState by mutableStateOf<SelectorDialogState>(SelectorDialogState.Closed)
        private set

    var menuState by mutableStateOf(service.menu)
        private set

    init {
        syncMenu()

        viewModelScope.launch {
            service.stateObservable.asFlow().collect {
                syncState(it)
            }
        }

        service.start()
    }

    override fun onCleared() {
        service.stop()
    }

    fun onErrorClick() {
        service.refresh()
    }

    fun showSelectorMenu() {
        selectorDialogState =
            SelectorDialogState.Opened(Select(service.sortingField, service.sortingFields))
    }

    fun onSelectorDialogDismiss() {
        selectorDialogState = SelectorDialogState.Closed
    }

    fun onSelectSortingField(sortingField: SortingField) {
        service.updateSortingField(sortingField)
        selectorDialogState = SelectorDialogState.Closed
        syncMenu()
    }

    fun marketFieldSelected(marketField: MarketField) {
        service.marketField = marketField

        syncMarketViewItems()
        syncMenu()

        stat(page = StatPage.AdvancedSearchResults, event = StatEvent.SwitchField(marketField.statField))
    }

    fun onAddFavorite(uid: String) {
        service.addFavorite(uid)
    }

    fun onRemoveFavorite(uid: String) {
        service.removeFavorite(uid)
    }

    private fun syncState(state: DataState<List<MarketItemWrapper>>) {
        viewModelScope.launch {
            state.viewState?.let {
                viewState = it
            }

            state.dataOrNull?.let {
                marketItems = it

                syncMarketViewItems()
            }

            syncMenu()
        }
    }

    private fun syncMenu() {
        menuState = service.menu
    }

    private fun syncMarketViewItems() {
        viewItemsState = marketItems.map {
            MarketViewItem.create(it.marketItem, service.marketField, it.favorited)
        }.toList()
    }

}
