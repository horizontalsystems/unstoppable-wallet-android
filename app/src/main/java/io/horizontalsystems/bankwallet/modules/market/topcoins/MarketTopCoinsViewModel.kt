package io.horizontalsystems.bankwallet.modules.market.topcoins

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.modules.market.topcoins.MarketTopCoinsModule.Header
import io.horizontalsystems.bankwallet.modules.market.topcoins.MarketTopCoinsModule.Menu
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.modules.market.topcoins.MarketTopCoinsModule.ViewItemState
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MarketTopCoinsViewModel(
    private val service: MarketTopCoinsService,
    private var marketField: MarketField = MarketField.MarketCap
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private val marketFields = MarketField.values().toList()
    private var marketItems: List<MarketItem> = listOf()

    val headerLiveData = MutableLiveData<Header>()
    val menuLiveData = MutableLiveData<Menu>()
    val viewStateLiveData = MutableLiveData<ViewItemState>()
    val loadingLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData<String?>()
    val isRefreshingLiveData = MutableLiveData<Boolean>()

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

    private fun syncState(state: DataState<List<MarketItem>>) {
        loadingLiveData.postValue(state is DataState.Loading)

        if (state is DataState.Success) {
            marketItems = state.data

            syncMarketViewItems()
        } else if (state is DataState.Error) {
            viewStateLiveData.postValue(ViewItemState.Error(convertErrorMessage(state.error)))
        }

        syncMenu()
    }

    private fun syncHeader() {
        headerLiveData.postValue(
            Header(
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
        viewStateLiveData.postValue(
            ViewItemState.Data(
                marketItems.map {
                    MarketTopCoinsModule.MarketViewItem.create(it, service.baseCurrency, marketField)
                }
            )
        )
    }

    private fun convertErrorMessage(it: Throwable): String {
        return it.message ?: it.javaClass.simpleName
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
    }

    fun onSelectTopMarket(topMarket: TopMarket) {
        service.setTopMarket(topMarket)
    }

    fun onSelectMarketField(marketField: MarketField) {
        this.marketField = marketField

        syncMarketViewItems()
        syncMenu()
    }

    fun refresh(){
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }
}
