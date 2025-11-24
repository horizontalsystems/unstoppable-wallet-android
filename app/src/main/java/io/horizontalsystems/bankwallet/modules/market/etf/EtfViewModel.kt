package io.horizontalsystems.bankwallet.modules.market.etf

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.chart.stringResId
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.market.etf.EtfModule.EtfViewItem
import io.horizontalsystems.bankwallet.modules.market.etf.EtfModule.RankedEtf
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabItem
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

    val sortByOptions = listOf(
        EtfModule.SortBy.HighestAssets,
        EtfModule.SortBy.LowestAssets,
        EtfModule.SortBy.Inflow,
        EtfModule.SortBy.Outflow
    )
    private val timePeriods = listOf(
        HsTimePeriod.Month1,
        HsTimePeriod.Month3,
        HsTimePeriod.Month6,
        HsTimePeriod.Year1,
    )
    private var viewState: ViewState = ViewState.Loading
    private var isRefreshing: Boolean = false
    private var viewItems: List<EtfViewItem> = listOf()
    private var marketDataJob: Job? = null
    private var sortBy: EtfModule.SortBy = sortByOptions.first()
    private var cachedEtfs: List<RankedEtf> = listOf()
    private var chartDataLoading = true
    private var etfPoints = listOf<EtfPoint>()
    private var tabKey: String = "btc"
    private var chartTabItems = listOf<TabItem<HsTimePeriod?>>()
    private var currentChartPeriod: HsTimePeriod? = HsTimePeriod.Month1
    private val chartIntervals: List<HsTimePeriod?> = timePeriods + listOf<HsTimePeriod?>(null)
    private var listTimePeriod: EtfListTimePeriod = EtfListTimePeriod.OneDay

    override fun createState() = EtfModule.UiState(
        viewItems = viewItems,
        viewState = viewState,
        isRefreshing = isRefreshing,
        sortBy = sortBy,
        chartDataLoading = chartDataLoading,
        etfPoints = etfPoints,
        currency = currencyManager.baseCurrency,
        chartTabs = chartTabItems,
        selectedChartInterval = currentChartPeriod,
        listTimePeriod = listTimePeriod
    )

    init {
        setChartTabs(currentChartPeriod)
    }

    fun loadData(tabKey: String) {
        this.tabKey = tabKey
        cachedEtfs = listOf() // Reset cached ETFs to ensure fresh data
        fetchChartData()
        syncItems()
    }

    private fun fetchChartData() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                etfPoints = marketKit.etfPoints(
                    tabKey,
                    currencyManager.baseCurrency.code,
                    currentChartPeriod?.value ?: "all"
                ).await()
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
                cachedEtfs = marketKit.etfs(tabKey, currencyManager.baseCurrency.code).await()
                    .sortedByDescending { it.totalAssets }
                    .mapIndexed { index, etf -> RankedEtf(etf, index + 1) }
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
                it.etf.priceChangeValue(listTimePeriod)
            }

            EtfModule.SortBy.Outflow -> cachedEtfs.sortedBy { it.etf.priceChangeValue(listTimePeriod) }
        }
        viewItems = sorted.map { etf ->
            etfViewItem(etf, listTimePeriod)
        }
    }

    private fun etfViewItem(rankedEtf: RankedEtf, listTimePeriod: EtfListTimePeriod) = EtfViewItem(
        title = rankedEtf.etf.ticker,
        iconUrl = "https://cdn.blocksdecoded.com/etf-tresuries/${rankedEtf.etf.ticker}@3x.png",
        subtitle = rankedEtf.etf.name,
        value = rankedEtf.etf.totalAssets?.let {
            App.numberFormatter.formatFiatShort(it, currencyManager.baseCurrency.symbol, 0)
        },
        subvalue = rankedEtf.etf.priceChangeValue(listTimePeriod)?.let {
            MarketDataValue.Diff(
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

    fun onSelectTimeDuration(selectedTimeDuration: EtfListTimePeriod) {
        listTimePeriod = selectedTimeDuration
        syncItems()
    }

    fun onSelectSortBy(selected: EtfModule.SortBy) {
        sortBy = selected
        syncItems()
    }

    fun onSelectChartInterval(interval: HsTimePeriod?) {
        currentChartPeriod = interval
        setChartTabs(interval)
        fetchChartData()
    }

    private fun setChartTabs(interval: HsTimePeriod?) {
        chartTabItems = chartIntervals.map {
            val titleResId = it?.stringResId ?: R.string.CoinPage_TimeDuration_All
            TabItem(Translator.getString(titleResId), it == interval, it)
        }
    }

}

private fun Etf.priceChangeValue(timeDuration: EtfListTimePeriod): BigDecimal? {
    return when (timeDuration) {
        EtfListTimePeriod.OneDay -> inflows[HsTimePeriod.Day1]
        EtfListTimePeriod.SevenDay -> inflows[HsTimePeriod.Week1]
        EtfListTimePeriod.ThirtyDay -> inflows[HsTimePeriod.Month1]
        EtfListTimePeriod.ThreeMonths -> inflows[HsTimePeriod.Month3]
        EtfListTimePeriod.All -> totalInflow
    }
}
