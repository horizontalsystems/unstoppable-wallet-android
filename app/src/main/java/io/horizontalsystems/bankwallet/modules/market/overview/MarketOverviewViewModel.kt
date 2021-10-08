package io.horizontalsystems.bankwallet.modules.market.overview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.MarketModule.TopMarket
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.BoardContent
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.BoardHeader
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.BoardItem
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.bankwallet.ui.compose.components.ToggleIndicator
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

class MarketOverviewViewModel(
    private val service: MarketOverviewService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val stateLiveData = MutableLiveData<MarketOverviewModule.State>()

    val toastLiveData = SingleLiveEvent<String>()

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val disposable = CompositeDisposable()

    private var topGainersMarket = TopMarket.Top250
    private var topLosersMarket = TopMarket.Top250

    init {
        service.stateObservable
            .subscribeIO {
                syncState(it)
            }
            .let {
                disposable.add(it)
            }
    }

    private fun syncState(state: MarketOverviewService.State) {
        val itemsEmpty = service.marketItems.isEmpty()

        when (state) {
            MarketOverviewService.State.Loading -> {
                if (itemsEmpty) {
                    stateLiveData.postValue(MarketOverviewModule.State.Loading)
                }
            }
            MarketOverviewService.State.Loaded -> {
                stateLiveData.postValue(MarketOverviewModule.State.Data(getBoardsData()))
            }
            is MarketOverviewService.State.Error -> {
                if (itemsEmpty) {
                    stateLiveData.postValue(MarketOverviewModule.State.Error)
                } else {
                    toastLiveData.postValue(convertErrorMessage(state.error))
                }
            }
        }
    }

    fun onToggleTopBoardSize(listType: MarketModule.ListType) {
        when (listType) {
            MarketModule.ListType.TopGainers -> {
                topGainersMarket = topGainersMarket.next()
            }
            MarketModule.ListType.TopLosers -> {
                topLosersMarket = topLosersMarket.next()
            }
        }
        stateLiveData.postValue(MarketOverviewModule.State.Data(getBoardsData()))
    }

    private fun getBoardsData(): List<BoardItem> {
        val marketItems = service.marketItems

        if (marketItems.isEmpty()) return listOf()

        val topGainersBoard = getBoardItem(MarketModule.ListType.TopGainers, marketItems)
        val topLosersBoard = getBoardItem(MarketModule.ListType.TopLosers, marketItems)

        return listOf(topGainersBoard, topLosersBoard)
    }

    private fun getBoardItem(type: MarketModule.ListType, marketItems: List<MarketItem>): BoardItem {
        val topMarket = when (type) {
            MarketModule.ListType.TopGainers -> topGainersMarket
            MarketModule.ListType.TopLosers -> topLosersMarket
        }
        val topList = marketItems
            .subList(0, min(marketItems.size, topMarket.value))
            .sort(type.sortingField)
            .subList(0, min(marketItems.size, 5))
            .map { MarketViewItem.create(it, type.marketField) }

        val topGainersHeader = BoardHeader(
            getSectionTitle(type),
            getSectionIcon(type),
            getToggleButton(topMarket)
        )
        val topBoardList = BoardContent(topList, type)
        return BoardItem(topGainersHeader, topBoardList, type)
    }

    private fun getToggleButton(topMarket: TopMarket): MarketListHeaderView.ToggleButton {
        val options = TopMarket.values().map { "${it.value}" }

        return MarketListHeaderView.ToggleButton(
            title = options[topMarket.ordinal],
            indicators = options.mapIndexed { index, _ -> ToggleIndicator(index == topMarket.ordinal) }
        )
    }

    private fun getSectionTitle(type: MarketModule.ListType): Int {
        return when (type) {
            MarketModule.ListType.TopGainers -> R.string.RateList_TopGainers
            MarketModule.ListType.TopLosers -> R.string.RateList_TopLosers
        }
    }

    private fun getSectionIcon(type: MarketModule.ListType): Int {
        return when (type) {
            MarketModule.ListType.TopGainers -> R.drawable.ic_circle_up_20
            MarketModule.ListType.TopLosers -> R.drawable.ic_circle_down_20
        }
    }

    private fun convertErrorMessage(it: Throwable): String {
        return it.message ?: it.javaClass.simpleName
    }

    fun onErrorClick() {
        service.refresh()
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposable.clear()
    }

    fun refresh() {
        if (_isRefreshing.value != null && _isRefreshing.value == true) {
            return
        }

        viewModelScope.launch {
            service.refresh()
            // A fake 2 seconds 'refresh'
            _isRefreshing.postValue(true)
            delay(2300)
            _isRefreshing.postValue(false)
        }
    }
}
