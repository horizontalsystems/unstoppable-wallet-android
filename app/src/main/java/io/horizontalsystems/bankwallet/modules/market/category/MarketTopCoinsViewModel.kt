package io.horizontalsystems.bankwallet.modules.market.category

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.modules.market.category.MarketTopCoinsModule.ViewState
import io.horizontalsystems.bankwallet.modules.market.category.MarketTopCoinsService.State
import io.horizontalsystems.bankwallet.ui.compose.components.ToggleIndicator
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.marketkit.models.CoinCategory
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MarketTopCoinsViewModel(
    private val service: MarketTopCoinsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val stateLiveData = MutableLiveData<ViewState>()

    private val disposable = CompositeDisposable()

    val sortingFields: Array<SortingField> = SortingField.values()

    private val _coinCategory = MutableLiveData(service.coinCategory)
    val coinCategory: LiveData<CoinCategory?> = _coinCategory

    private val _sortingField = MutableLiveData(sortingFields.first())
    val sortingField: LiveData<SortingField> = _sortingField

    private var marketField = MarketField.PriceDiff
    private var topMarket = TopMarket.Top250

    private val _marketFieldButton = MutableLiveData(getMarketFieldButton())
    val marketFieldButton: LiveData<MarketListHeaderView.ToggleButton> = _marketFieldButton

    private val _topMarketButton = MutableLiveData(getTopMarketButton())
    val topMarketButton: LiveData<MarketListHeaderView.ToggleButton> = _topMarketButton


    init {
        fetchCoins()

        service.stateObservable
            .subscribeIO {
                syncState(it)
            }
            .let {
                disposable.add(it)
            }
    }

    private fun fetchCoins() {
        val nonNullSortingField = _sortingField.value ?: return
//        val sortOrder = getSortOrder(nonNullSortingField)
        val top = topMarket.value

        service.fetchCoinList(top)
    }

//    private fun getSortOrder(sortingField: SortingField): MarketInfo.Order {
//        return when (sortingField) {
//            SortingField.HighestCap -> MarketInfo.Order(
//                MarketInfo.OrderField.MarketCap,
//                MarketInfo.OrderDirection.Descending
//            )
//            SortingField.LowestCap -> MarketInfo.Order(
//                MarketInfo.OrderField.MarketCap,
//                MarketInfo.OrderDirection.Ascending
//            )
//            SortingField.TopGainers -> MarketInfo.Order(
//                MarketInfo.OrderField.PriceChange,
//                MarketInfo.OrderDirection.Descending
//            )
//            SortingField.TopLosers -> MarketInfo.Order(
//                MarketInfo.OrderField.PriceChange,
//                MarketInfo.OrderDirection.Ascending
//            )
//            SortingField.HighestVolume -> MarketInfo.Order(
//                MarketInfo.OrderField.TotalVolume,
//                MarketInfo.OrderDirection.Descending
//            )
//            SortingField.LowestVolume -> MarketInfo.Order(
//                MarketInfo.OrderField.TotalVolume,
//                MarketInfo.OrderDirection.Ascending
//            )
//        }
//    }

    private fun syncState(state: State) {
        when (state) {
            State.Loading -> stateLiveData.postValue(ViewState.Loading)
            State.Loaded -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val viewItems = service.marketInfoItems.map {
                        MarketTopCoinsModule.ViewItem.create(it, service.baseCurrency, marketField)
                    }
                    stateLiveData.postValue(ViewState.Data(viewItems))
                }
            }
            is State.Error -> stateLiveData.postValue(
                ViewState.Error(convertErrorMessage(state.error))
            )
        }
    }

    private fun getMarketFieldButton(): MarketListHeaderView.ToggleButton {
        val marketFields = MarketField.values()

        return MarketListHeaderView.ToggleButton(
            title = marketField.name,
            indicators = marketFields.mapIndexed { index, _ -> ToggleIndicator(index == marketField.ordinal) }
        )
    }

    private fun getTopMarketButton(): MarketListHeaderView.ToggleButton {
        val topNumbers = TopMarket.values()

        return MarketListHeaderView.ToggleButton(
            title = topMarket.value.toString(),
            indicators = topNumbers.mapIndexed { index, _ -> ToggleIndicator(index == topMarket.ordinal) }
        )
    }

    private fun convertErrorMessage(it: Throwable): String {
        return it.message ?: it.javaClass.simpleName
    }

    override fun onCleared() {
        disposable.clear()
        clearables.forEach(Clearable::clear)
    }

    fun updateSorting(sortingField: SortingField) {
        _sortingField.value = sortingField
        fetchCoins()
    }

    fun onMarketFieldButtonClick() {
        marketField = marketField.next()
        _marketFieldButton.value = getMarketFieldButton()
        fetchCoins()
    }

    fun onTopMarketButtonClick() {
        topMarket = topMarket.next()
        _topMarketButton.value = getTopMarketButton()
        fetchCoins()
    }
}
