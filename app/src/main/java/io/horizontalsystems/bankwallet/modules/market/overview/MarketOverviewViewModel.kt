package io.horizontalsystems.bankwallet.modules.market.overview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.*
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.BoardContent
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.BoardHeader
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.BoardItem
import io.horizontalsystems.bankwallet.ui.compose.components.ToggleIndicator
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MarketOverviewViewModel(
        private val service: MarketOverviewService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val stateLiveData = MutableLiveData<MarketOverviewModule.State>()

    val toastLiveData = SingleLiveEvent<String>()

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val disposable = CompositeDisposable()

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

    private fun getBoardsData(): List<BoardItem> {
        val topGainersBoard = getBoardItem(MarketModule.ListType.TopGainers)
        val topLosersBoard = getBoardItem(MarketModule.ListType.TopLosers)

        return listOf(topGainersBoard, topLosersBoard)
    }

    private fun getBoardItem(type: MarketModule.ListType): BoardItem {
        val sortingField: SortingField = getSortingField(type)

        val topGainersList = service.marketItems
            .sort(sortingField).subList(0, 5)
            .map { MarketViewItem.create(it, MarketField.PriceDiff) }

        val topGainersHeader = BoardHeader(
            getSectionTitle(type),
            getSectionIcon(type),
            getToggleButton(0)
        )
        val topGainersBoardList = BoardContent(topGainersList, type)
        return BoardItem(topGainersHeader, topGainersBoardList, type)
    }

    private fun getSortingField(type: MarketModule.ListType): SortingField {
        return when(type){
            MarketModule.ListType.TopGainers -> SortingField.TopGainers
            MarketModule.ListType.TopLosers -> SortingField.TopLosers
        }
    }

    private fun getToggleButton(selectedIndex: Int): MarketListHeaderView.ToggleButton {
        val options = listOf("250", "500", "1000")

        return MarketListHeaderView.ToggleButton(
            title = options[selectedIndex],
            indicators = options.mapIndexed { index, _ -> ToggleIndicator(index == selectedIndex) }
        )
    }

    private fun getSectionTitle(type: MarketModule.ListType): Int{
        return when(type){
            MarketModule.ListType.TopGainers -> R.string.RateList_TopGainers
            MarketModule.ListType.TopLosers -> R.string.RateList_TopLosers
        }
    }

    private fun getSectionIcon(type: MarketModule.ListType): Int{
        return when(type){
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
