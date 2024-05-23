package io.horizontalsystems.bankwallet.modules.market.topplatforms

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TopPlatformsViewModel(
    private val service: TopPlatformsService,
    timeDuration: TimeDuration?,
) : ViewModelUiState<TopPlatformsModule.UiState>() {

    val sortingOptions = listOf(
        SortingField.HighestCap,
        SortingField.LowestCap,
        SortingField.TopGainers,
        SortingField.TopLosers
    )

    val periods = listOf(
        TimeDuration.SevenDay,
        TimeDuration.ThirtyDay,
        TimeDuration.ThreeMonths,
    )

    private var sortingField = SortingField.TopGainers

    private var timePeriod = timeDuration ?: periods.first()

    private var viewItems = emptyList<TopPlatformViewItem>()

    private var viewState: ViewState = ViewState.Loading

    private var isRefreshing = false


    init {
        viewModelScope.launch {
            sync(false)
        }
    }

    override fun createState(): TopPlatformsModule.UiState {
        return TopPlatformsModule.UiState(
            sortingField = sortingField,
            timePeriod = timePeriod,
            viewItems = viewItems,
            viewState = viewState,
            isRefreshing = isRefreshing
        )
    }

    private fun sync(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                val topPlatformItems =
                    service.getTopPlatforms(sortingField, timePeriod, forceRefresh)
                viewItems = getViewItems(topPlatformItems)
                viewState = ViewState.Success
            } catch (e: Throwable) {
                viewState = ViewState.Error(e)
            }
            emitState()
        }
    }

    private fun getViewItems(topPlatformItems: List<TopPlatformItem>): List<TopPlatformViewItem> {
        return topPlatformItems.map { item ->
            TopPlatformViewItem(
                platform = item.platform,
                subtitle = Translator.getString(
                    R.string.MarketTopPlatforms_Protocols,
                    item.protocols
                ),
                marketCap = App.numberFormatter.formatFiatShort(
                    item.marketCap,
                    service.baseCurrency.symbol,
                    2
                ),
                marketCapDiff = item.changeDiff,
                rank = item.rank.toString(),
                rankDiff = item.rankDiff,
            )
        }
    }

    private fun refreshWithMinLoadingSpinnerPeriod() {
        viewModelScope.launch {
            isRefreshing = true
            sync(true)
            delay(1000)
            isRefreshing = false
        }
    }

    fun onSelectSortingField(sortingField: SortingField) {
        this.sortingField = sortingField
        sync()
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onTimePeriodSelect(timePeriod: TimeDuration) {
        this.timePeriod = timePeriod
        sync()
    }
}
