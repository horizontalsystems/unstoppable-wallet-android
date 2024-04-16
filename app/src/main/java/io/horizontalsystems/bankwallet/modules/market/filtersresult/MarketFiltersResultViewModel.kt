package io.horizontalsystems.bankwallet.modules.market.filtersresult

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.category.MarketItemWrapper
import io.horizontalsystems.bankwallet.modules.market.topcoins.SelectorDialogState
import io.horizontalsystems.bankwallet.ui.compose.Select
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
