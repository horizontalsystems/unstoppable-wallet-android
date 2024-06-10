package io.horizontalsystems.bankwallet.modules.market.category

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statField
import io.horizontalsystems.bankwallet.core.stats.statSortType
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.topcoins.SelectorDialogState
import io.horizontalsystems.bankwallet.ui.compose.Select
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class MarketCategoryViewModel(
    private val service: MarketCategoryService,
) : ViewModel() {

    private val marketFields = MarketField.values().toList()
    private var marketItems: List<MarketItemWrapper> = listOf()
    private var marketField = MarketField.PriceDiff

    val headerLiveData = MutableLiveData<MarketModule.Header>()
    val menuLiveData = MutableLiveData<MarketCategoryModule.Menu>()
    val viewStateLiveData = MutableLiveData<ViewState>()
    val viewItemsLiveData = MutableLiveData<List<MarketViewItem>>()
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val selectorDialogStateLiveData = MutableLiveData<SelectorDialogState>()

    init {
        syncHeader()
        syncMenu()

        viewModelScope.launch {
            service.stateObservable.asFlow().collect {
                syncState(it)
            }
        }

        service.start()
    }

    private fun syncState(state: DataState<List<MarketItemWrapper>>) {
        viewStateLiveData.postValue(state.viewState)

        state.dataOrNull?.let {
            marketItems = it

            syncMarketViewItems()
        }

        syncMenu()
    }

    private fun syncHeader() {
        headerLiveData.postValue(
            MarketModule.Header(
                service.coinCategoryName,
                service.coinCategoryDescription,
                ImageSource.Remote(service.coinCategoryImageUrl)
            )
        )
    }

    private fun syncMenu() {
        menuLiveData.postValue(
            MarketCategoryModule.Menu(
                Select(service.sortingField, service.sortingFields),
                Select(marketField, marketFields)
            )
        )
    }

    private fun syncMarketViewItems() {
        viewItemsLiveData.postValue(
            marketItems.map {
                MarketViewItem.create(it.marketItem, it.favorited)
            }
        )
    }

    private fun refreshWithMinLoadingSpinnerPeriod() {
        service.refresh()
        viewModelScope.launch {
            isRefreshingLiveData.postValue(true)
            delay(1000)
            isRefreshingLiveData.postValue(false)
        }
    }

    fun onSelectSortingField(sortingField: SortingField) {
        service.setSortingField(sortingField)
        selectorDialogStateLiveData.postValue(SelectorDialogState.Closed)

        stat(page = StatPage.CoinCategory, event = StatEvent.SwitchSortType(sortingField.statSortType))
    }

    fun onSelectMarketField(marketField: MarketField) {
        this.marketField = marketField

        syncMarketViewItems()
        syncMenu()

        stat(page = StatPage.CoinCategory, event = StatEvent.SwitchField(marketField.statField))
    }

    fun onSelectorDialogDismiss() {
        selectorDialogStateLiveData.postValue(SelectorDialogState.Closed)
    }

    fun showSelectorMenu() {
        selectorDialogStateLiveData.postValue(
            SelectorDialogState.Opened(Select(service.sortingField, service.sortingFields))
        )
    }

    fun refresh(){
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    override fun onCleared() {
        service.stop()
    }

    fun onAddFavorite(uid: String) {
        service.addFavorite(uid)

        stat(page = StatPage.CoinCategory, event = StatEvent.AddToWatchlist(uid))
    }

    fun onRemoveFavorite(uid: String) {
        service.removeFavorite(uid)

        stat(page = StatPage.CoinCategory, event = StatEvent.RemoveFromWatchlist(uid))
    }
}
