package io.horizontalsystems.bankwallet.modules.market.filtersresult

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.category.MarketItemWrapper
import io.horizontalsystems.bankwallet.modules.market.topcoins.SelectorDialogState
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.reactivex.disposables.CompositeDisposable

class MarketFiltersResultViewModel(
    private val service: MarketFiltersResultService,
) : ViewModel() {

    private var marketItems: List<MarketItemWrapper> = listOf()

    val viewStateLiveData = MutableLiveData<ViewState>()
    val viewItemsLiveData = MutableLiveData<List<MarketViewItem>>()
    val selectorDialogStateLiveData = MutableLiveData<SelectorDialogState>()

    var menuState by mutableStateOf(service.menu)
        private set

    private val disposable = CompositeDisposable()

    init {
        syncMenu()

        service.stateObservable
            .subscribeIO {
                syncState(it)
            }
            .let {
                disposable.add(it)
            }

        service.start()
    }

    override fun onCleared() {
        service.stop()
        disposable.clear()
    }

    fun onErrorClick() {
        service.refresh()
    }

    fun showSelectorMenu() {
        selectorDialogStateLiveData.postValue(
            SelectorDialogState.Opened(Select(service.sortingField, service.sortingFields))
        )
    }

    fun onSelectorDialogDismiss() {
        selectorDialogStateLiveData.postValue(SelectorDialogState.Closed)
    }

    fun onSelectSortingField(sortingField: SortingField) {
        service.updateSortingField(sortingField)
        selectorDialogStateLiveData.postValue(SelectorDialogState.Closed)
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
        viewStateLiveData.postValue(state.viewState)

        state.dataOrNull?.let {
            marketItems = it

            syncMarketViewItems()
        }

        syncMenu()
    }

    private fun syncMenu() {
        menuState = service.menu
    }

    private fun syncMarketViewItems() {
        viewItemsLiveData.postValue(
            marketItems.map {
                MarketViewItem.create(it.marketItem, service.marketField, it.favorited)
            }
        )
    }

}
