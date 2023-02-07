package cash.p.terminal.modules.market.favorites

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.market.MarketField
import cash.p.terminal.modules.market.MarketViewItem
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.category.MarketItemWrapper
import cash.p.terminal.modules.market.favorites.MarketFavoritesModule.SelectorDialogState
import cash.p.terminal.modules.market.favorites.MarketFavoritesModule.ViewItem
import cash.p.terminal.ui.compose.Select
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MarketFavoritesViewModel(
    private val service: MarketFavoritesService,
    private val menuService: MarketFavoritesMenuService
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val marketFieldTypes = MarketField.values().toList()
    private var marketField: MarketField by menuService::marketField
    private var marketItemsWrapper: List<MarketItemWrapper> = listOf()

    private val marketFieldSelect: Select<MarketField>
        get() = Select(marketField, marketFieldTypes)

    private val sortingFieldSelect: Select<SortingField>
        get() = Select(service.sortingField, service.sortingFieldTypes)

    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val viewItemLiveData = MutableLiveData<ViewItem>()
    val sortingFieldSelectorStateLiveData = MutableLiveData<SelectorDialogState>()

    init {
        service.marketItemsObservable
            .subscribeIO { state ->
                when (state) {
                    is DataState.Success -> {
                        viewStateLiveData.postValue(ViewState.Success)
                        marketItemsWrapper = state.data
                        syncViewItem()
                    }
                    is DataState.Error -> {
                        viewStateLiveData.postValue(ViewState.Error(state.error))
                    }
                    DataState.Loading -> {}
                }
            }.let { disposables.add(it) }

        service.start()
    }

    private fun refreshWithMinLoadingSpinnerPeriod() {
        service.refresh()
        viewModelScope.launch {
            isRefreshingLiveData.postValue(true)
            delay(1000)
            isRefreshingLiveData.postValue(false)
        }
    }

    private fun syncViewItem() {
        viewItemLiveData.postValue(
            ViewItem(
                sortingFieldSelect,
                marketFieldSelect,
                marketItemsWrapper.map {
                    MarketViewItem.create(it.marketItem, marketField, true)
                }
            )
        )
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onClickSortingField() {
        sortingFieldSelectorStateLiveData.postValue(SelectorDialogState.Opened(sortingFieldSelect))
    }

    fun onSelectSortingField(sortingField: SortingField) {
        service.sortingField = sortingField
        sortingFieldSelectorStateLiveData.postValue(SelectorDialogState.Closed)
    }

    fun onSelectMarketField(marketField: MarketField) {
        this.marketField = marketField

        syncViewItem()
    }

    fun onSortingFieldDialogDismiss() {
        sortingFieldSelectorStateLiveData.postValue(SelectorDialogState.Closed)
    }

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }

    fun removeFromFavorites(uid: String) {
        service.removeFavorite(uid)
    }
}
