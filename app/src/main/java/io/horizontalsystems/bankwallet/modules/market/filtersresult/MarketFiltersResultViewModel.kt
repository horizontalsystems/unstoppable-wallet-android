package io.horizontalsystems.bankwallet.modules.market.filtersresult

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.SignalsControlManager
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketItemWrapper
import io.horizontalsystems.bankwallet.modules.market.filters.IMarketListFetcher
import io.horizontalsystems.bankwallet.ui.compose.Select
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

@HiltViewModel(assistedFactory = MarketFiltersResultViewModel.Factory::class)
class MarketFiltersResultViewModel @AssistedInject constructor(
    @Assisted fetcher: IMarketListFetcher,
    marketFavoritesManager: MarketFavoritesManager,
    localStorage: ILocalStorage,
    marketKit: MarketKitWrapper,
) : ViewModelUiState<MarketFiltersUiState>() {

    @AssistedFactory
    interface Factory {
        fun create(fetcher: IMarketListFetcher): MarketFiltersResultViewModel
    }

    private val service = MarketFiltersResultService(
        fetcher,
        marketFavoritesManager,
        SignalsControlManager(localStorage),
        marketKit
    )

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
        selectSortingField = Select(service.sortingField, service.sortingFields),
        showSignal = service.showSignals
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
            MarketViewItem.create(
                marketItem = itemWrapper.marketItem,
                favorited = itemWrapper.favorited,
                advice = itemWrapper.signal
            )
        }.toList()
    }

    fun showSignals() {
        service.showSignals()
        emitState()
    }

    fun hideSignals() {
        service.hideSignals()
        emitState()
    }

}

data class MarketFiltersUiState(
    val viewItems: List<MarketViewItem>,
    val viewState: ViewState,
    val sortingField: SortingField,
    val selectSortingField: Select<SortingField>,
    val showSignal: Boolean
)
