package io.horizontalsystems.bankwallet.modules.market.favorites

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.category.MarketItemWrapper
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketFavoritesModule.Period
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketFavoritesModule.ViewItem
import io.horizontalsystems.bankwallet.ui.compose.Select
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class MarketFavoritesViewModel(
    private val service: MarketFavoritesService,
) : ViewModel() {

    private var marketItemsWrapper: List<MarketItemWrapper> = listOf()
    private val timeDurationOptions: List<Period> = listOf(
        Period.OneDay,
        Period.SevenDay,
        Period.ThirtyDay,
    )

    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val viewItemLiveData = MutableLiveData<ViewItem>()

    init {
        viewModelScope.launch {
            service.marketItemsObservable.asFlow().collect { state ->
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
            }
        }

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
                sortingDescending = service.sortDescending,
                periodSelect = Select(service.period, timeDurationOptions),
                marketItems = marketItemsWrapper.map {
                    MarketViewItem.create(it.marketItem, true)
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

    fun onSortToggle() {
        service.sortDescending = !service.sortDescending
    }

    fun onSelectTimeDuration(period: Period) {
        service.period = period
    }

    override fun onCleared() {
        service.stop()
    }

    fun removeFromFavorites(uid: String) {
        service.removeFavorite(uid)
    }
}
