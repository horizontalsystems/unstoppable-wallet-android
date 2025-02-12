package io.horizontalsystems.bankwallet.modules.market.etf

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.market.etf.EtfModule.EtfViewItem
import io.horizontalsystems.bankwallet.modules.market.etf.EtfModule.RankedEtf
import io.horizontalsystems.marketkit.models.Etf
import io.horizontalsystems.marketkit.models.EtfPoint
import io.horizontalsystems.marketkit.models.HsTimePeriod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

class EtfViewModel(
    private val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper
) : ViewModelUiState<EtfModule.UiState>() {

    val timeDurations = listOf(
        TimeDuration.OneDay,
        TimeDuration.SevenDay,
        TimeDuration.ThirtyDay,
        TimeDuration.ThreeMonths,
    )
    val sortByOptions = listOf(
        EtfModule.SortBy.HighestAssets,
        EtfModule.SortBy.LowestAssets,
        EtfModule.SortBy.Inflow,
        EtfModule.SortBy.Outflow
    )
    private var viewState: ViewState = ViewState.Loading
    private var isRefreshing: Boolean = false
    private var viewItems: List<EtfViewItem> = listOf()
    private var marketDataJob: Job? = null
    private var sortBy: EtfModule.SortBy = sortByOptions.first()
    private var timeDuration: TimeDuration = timeDurations.first()
    private var cachedEtfs: List<RankedEtf> = listOf()
    private var chartDataLoading = true
    private var etfPoints = listOf<EtfPoint>()

    override fun createState() = EtfModule.UiState(
        viewItems = viewItems,
        viewState = viewState,
        isRefreshing = isRefreshing,
        timeDuration = timeDuration,
        sortBy = sortBy,
        chartDataLoading = chartDataLoading,
        etfPoints = etfPoints,
        currency = currencyManager.baseCurrency
    )

    init {
        syncItems()
        fetchChartData()
    }

    private fun fetchChartData() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                etfPoints = marketKit.etfPoints(currencyManager.baseCurrency.code).await()
                    .sortedBy { it.date }
                chartDataLoading = false

                emitState()
            } catch (e: Throwable) {
                chartDataLoading = false
                emitState()
            }
        }
    }

    private fun syncItems() {
        if (cachedEtfs.isEmpty()) {
            fetchEtfs()
        } else {
            updateViewItems()
            emitState()
        }
    }

    private fun fetchEtfs() {
        marketDataJob?.cancel()
        marketDataJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                cachedEtfs = marketKit.etfs(currencyManager.baseCurrency.code).await()
                    .sortedByDescending { it.totalAssets }
                    .mapIndexed{ index, etf -> RankedEtf(etf, index + 1) }
                updateViewItems()

                viewState = ViewState.Success
            } catch (e: CancellationException) {
                // no-op
            } catch (e: Throwable) {
                viewState = ViewState.Error(e)
            }
            emitState()
        }
    }

    private fun updateViewItems() {
        val sorted = when (sortBy) {
            EtfModule.SortBy.HighestAssets -> cachedEtfs.sortedByDescending { it.etf.totalAssets }
            EtfModule.SortBy.LowestAssets -> cachedEtfs.sortedBy { it.etf.totalAssets }
            EtfModule.SortBy.Inflow -> cachedEtfs.sortedByDescending {
                it.etf.priceChangeValue(
                    timeDuration
                )
            }

            EtfModule.SortBy.Outflow -> cachedEtfs.sortedBy { it.etf.priceChangeValue(timeDuration) }
        }
        viewItems = sorted.map { etf ->
            etfViewItem(etf, timeDuration)
        }
    }

    private fun etfViewItem(rankedEtf: RankedEtf, timeDuration: TimeDuration) = EtfViewItem(
        title = rankedEtf.etf.ticker,
        iconUrl = "https://cdn.blocksdecoded.com/etf-tresuries/${rankedEtf.etf.ticker}@3x.png",
        subtitle = rankedEtf.etf.name,
        value = rankedEtf.etf.totalAssets?.let {
            App.numberFormatter.formatFiatShort(it, currencyManager.baseCurrency.symbol, 0)
        },
        subvalue = rankedEtf.etf.priceChangeValue(timeDuration)?.let {
            MarketDataValue.DiffNew(
                Value.Currency(
                    CurrencyValue(currencyManager.baseCurrency, it)
                )
            )
        },
        rank = rankedEtf.rank.toString()
    )

    private fun refreshWithMinLoadingSpinnerPeriod() {
        isRefreshing = true
        emitState()
        fetchChartData()
        syncItems()
        viewModelScope.launch {
            delay(1000)
            isRefreshing = false
            emitState()
        }
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onSelectTimeDuration(selected: TimeDuration) {
        timeDuration = selected
        syncItems()
    }

    fun onSelectSortBy(selected: EtfModule.SortBy) {
        sortBy = selected
        syncItems()
    }

}

private fun Etf.priceChangeValue(timeDuration: TimeDuration): BigDecimal? {
    return when (timeDuration) {
        TimeDuration.OneDay -> inflows[HsTimePeriod.Day1]
        TimeDuration.SevenDay -> inflows[HsTimePeriod.Week1]
        TimeDuration.ThirtyDay -> inflows[HsTimePeriod.Month1]
        TimeDuration.ThreeMonths -> inflows[HsTimePeriod.Month3]
    }
}
