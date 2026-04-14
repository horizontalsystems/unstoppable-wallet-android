package com.quantum.wallet.bankwallet.modules.market.platform

import androidx.lifecycle.viewModelScope
import com.quantum.wallet.bankwallet.core.ViewModelUiState
import com.quantum.wallet.bankwallet.core.managers.MarketFavoritesManager
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.market.MarketItem
import com.quantum.wallet.bankwallet.modules.market.MarketViewItem
import com.quantum.wallet.bankwallet.modules.market.SortingField
import com.quantum.wallet.bankwallet.modules.market.TimeDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MarketPlatformViewModel(
    private val repository: MarketPlatformCoinsRepository,
    private val favoritesManager: MarketFavoritesManager,
) : ViewModelUiState<MarketPlatformUiState>() {

    val sortingFields = listOf(
        SortingField.HighestCap,
        SortingField.LowestCap,
        SortingField.TopGainers,
        SortingField.TopLosers,
    )

    val periods = listOf(
        TimeDuration.OneDay,
        TimeDuration.SevenDay,
        TimeDuration.ThirtyDay,
        TimeDuration.ThreeMonths,
    )

    private var sortingField: SortingField = SortingField.TopGainers
    private var viewState: ViewState = ViewState.Loading
    private var viewItems: List<MarketViewItem> = listOf()
    private var isRefreshing = false
    private var timePeriod = periods.first()

    init {
        sync()
    }

    override fun createState() = MarketPlatformUiState(
        viewItems = viewItems,
        viewState = viewState,
        sortingField = sortingField,
        timePeriod = timePeriod,
        isRefreshing = isRefreshing,
    )

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onSelectSortingField(sortingField: SortingField) {
        this.sortingField = sortingField
        sync()
    }

    fun onAddFavorite(coinUid: String) {
        favoritesManager.add(coinUid)
        sync()
    }

    fun onRemoveFavorite(coinUid: String) {
        favoritesManager.remove(coinUid)
        sync()
    }

    fun onTimePeriodSelect(timePeriod: TimeDuration) {
        this.timePeriod = timePeriod

        viewModelScope.launch {
            sync()
        }
    }

    private fun sync(forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            fetchFromRepository(forceRefresh)
        }
    }

    private suspend fun fetchFromRepository(forceRefresh: Boolean) {
        try {
            viewItems = repository.get(sortingField, timePeriod, forceRefresh).map {
                marketViewItem(it)
            }

            viewState = ViewState.Success
        } catch (e: Throwable) {
            viewState = ViewState.Error(e)
        }
        emitState()
    }

    private fun marketViewItem(item: MarketItem): MarketViewItem = MarketViewItem.create(
        marketItem = item,
        favorited = favoritesManager.getAll().map { it.coinUid }.contains(item.fullCoin.coin.uid)
    )

    private fun refreshWithMinLoadingSpinnerPeriod() {
        viewModelScope.launch {
            sync(true)

            isRefreshing = true
            delay(1000)
            isRefreshing = false
            emitState()
        }
    }
}

data class MarketPlatformUiState(
    val viewItems: List<MarketViewItem>,
    val viewState: ViewState,
    val sortingField: SortingField,
    val timePeriod: TimeDuration,
    val isRefreshing: Boolean,
)