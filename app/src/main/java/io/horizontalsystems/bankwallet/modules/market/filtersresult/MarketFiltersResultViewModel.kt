package io.horizontalsystems.bankwallet.modules.market.filtersresult

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.category.MarketItemWrapper
import io.horizontalsystems.bankwallet.ui.compose.Select
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
            MarketViewItem.create(itemWrapper.marketItem, itemWrapper.favorited)
        }.toList()
    }

}

data class MarketFiltersUiState(
    val viewItems: List<MarketViewItem>,
    val viewState: ViewState,
    val sortingField: SortingField,
    val selectSortingField: Select<SortingField>
)