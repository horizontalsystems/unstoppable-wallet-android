package cash.p.terminal.modules.market.topplatforms

import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.App
import io.horizontalsystems.core.ViewModelUiState
import cash.p.terminal.ui_compose.entities.ViewState
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.TimeDuration
import io.horizontalsystems.core.CurrencyManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TopPlatformsViewModel(
    private val repository: TopPlatformsRepository,
    private val currencyManager: CurrencyManager,
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

        viewModelScope.launch {
            currencyManager.baseCurrencyUpdatedFlow.collect {
                sync(true)
            }
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

    private suspend fun sync(forceRefresh: Boolean = false) {
        try {
            val topPlatformItems = repository.get(
                sortingField,
                timePeriod,
                currencyManager.baseCurrency.code,
                forceRefresh
            )
            viewItems = getViewItems(topPlatformItems)
            viewState = ViewState.Success
        } catch (e: Throwable) {
            viewState = ViewState.Error(e)
        }
        emitState()
    }

    private fun getViewItems(topPlatformItems: List<TopPlatformItem>): List<TopPlatformViewItem> {
        return topPlatformItems.map { item ->
            TopPlatformViewItem(
                platform = item.platform,
                subtitle = cash.p.terminal.strings.helpers.Translator.getString(
                    R.string.MarketTopPlatforms_Protocols,
                    item.protocols
                ),
                marketCap = App.numberFormatter.formatFiatShort(
                    item.marketCap,
                    currencyManager.baseCurrency.symbol,
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
            emitState()

            sync(true)

            delay(1000)
            isRefreshing = false
            emitState()
        }
    }

    fun onSelectSortingField(sortingField: SortingField) {
        this.sortingField = sortingField

        viewModelScope.launch {
            sync()
        }
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onTimePeriodSelect(timePeriod: TimeDuration) {
        this.timePeriod = timePeriod

        viewModelScope.launch {
            sync()
        }
    }
}
