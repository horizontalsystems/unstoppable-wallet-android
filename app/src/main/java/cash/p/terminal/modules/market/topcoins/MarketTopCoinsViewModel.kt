package cash.p.terminal.modules.market.topcoins

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.market.*
import cash.p.terminal.modules.market.category.MarketItemWrapper
import cash.p.terminal.modules.market.topcoins.MarketTopCoinsModule.Menu
import cash.p.terminal.ui.compose.Select
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MarketTopCoinsViewModel(
    private val service: MarketTopCoinsService,
    private var marketField: MarketField
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private val marketFields = MarketField.values().toList()
    private var marketItems: List<MarketItemWrapper> = listOf()

    val headerLiveData = MutableLiveData<MarketModule.Header>()
    val menuLiveData = MutableLiveData<Menu>()
    val viewItemsLiveData = MutableLiveData<List<MarketViewItem>>()
    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val selectorDialogStateLiveData = MutableLiveData<SelectorDialogState>()

    init {
        syncHeader()
        syncMenu()

        service.stateObservable
            .subscribeIO {
                syncState(it)
            }.let {
                disposables.add(it)
            }

        service.start()
    }

    private fun syncState(state: DataState<List<MarketItemWrapper>>) {
        state.viewState?.let {
            viewStateLiveData.postValue(it)
        }

        state.dataOrNull?.let {
            marketItems = it

            syncMarketViewItems()
        }

        syncMenu()
    }

    private fun syncHeader() {
        headerLiveData.postValue(
            MarketModule.Header(
                Translator.getString(R.string.Market_Category_TopCoins),
                Translator.getString(R.string.Market_Category_TopCoins_Description),
                ImageSource.Local(R.drawable.ic_top_coins)
            )
        )
    }

    private fun syncMenu() {
        menuLiveData.postValue(
            Menu(
                Select(service.sortingField, service.sortingFields),
                Select(service.topMarket, service.topMarkets),
                Select(marketField, marketFields)
            )
        )
    }

    private fun syncMarketViewItems() {
        viewItemsLiveData.postValue(
            marketItems.map {
                MarketViewItem.create(it.marketItem, marketField, it.favorited)
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
    }

    fun onSelectTopMarket(topMarket: TopMarket) {
        service.setTopMarket(topMarket)
    }

    fun onSelectMarketField(marketField: MarketField) {
        this.marketField = marketField

        syncMarketViewItems()
        syncMenu()
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }

    fun onSelectorDialogDismiss() {
        selectorDialogStateLiveData.postValue(SelectorDialogState.Closed)
    }

    fun showSelectorMenu() {
        selectorDialogStateLiveData.postValue(
            SelectorDialogState.Opened(Select(service.sortingField, service.sortingFields))
        )
    }

    fun onAddFavorite(coinUid: String) {
        service.addFavorite(coinUid)
    }

    fun onRemoveFavorite(coinUid: String) {
        service.removeFavorite(coinUid)
    }
}
