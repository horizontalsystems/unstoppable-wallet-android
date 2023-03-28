package io.horizontalsystems.bankwallet.modules.chart

import android.util.Range
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.viewState
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

open class ChartViewModel(
    private val service: AbstractChartService,
    private val valueFormatter: ChartModule.ChartNumberFormatter,
) : ViewModel() {

    private var tabItems = listOf<TabItem<HsTimePeriod?>>()
    private var chartHeaderView: ChartModule.ChartHeaderView? = null
    private var chartInfoData: ChartInfoData? = null
    private var loading = false
    private var viewState: ViewState = ViewState.Success

    var uiState by mutableStateOf(
        ChartUiState(
            tabItems = tabItems,
            chartHeaderView = chartHeaderView,
            chartInfoData = chartInfoData,
            loading = loading,
            viewState = viewState,
            hasVolumes = service.hasVolumes,
            chartViewType = service.chartViewType
        )
    )
        private set

    private val disposables = CompositeDisposable()

    init {
        loading = true
        emitState()

        service.chartTypeObservable
            .subscribeIO { chartType ->
                val tabItems = service.chartIntervals.map {
                    val titleResId = it?.stringResId ?: R.string.CoinPage_TimeDuration_All
                    TabItem(Translator.getString(titleResId), it == chartType.orElse(null), it)
                }
                this.tabItems = tabItems

                emitState()
            }
            .let {
                disposables.add(it)
            }

        service.chartPointsWrapperObservable
            .subscribeIO { chartItemsDataState ->
                chartItemsDataState.viewState?.let {
                    viewState = it
                }

                loading = false

                syncChartItems(chartItemsDataState.getOrNull())

                emitState()
            }
            .let {
                disposables.add(it)
            }

        viewModelScope.launch(Dispatchers.IO) {
            service.start()
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = ChartUiState(
                tabItems = tabItems,
                chartHeaderView = chartHeaderView,
                chartInfoData = chartInfoData,
                loading = loading,
                viewState = viewState,
                hasVolumes = service.hasVolumes,
                chartViewType = service.chartViewType,
            )
        }
    }

    fun onSelectChartInterval(chartInterval: HsTimePeriod?) {
        loading = true
        viewModelScope.launch {
            // Solution to prevent flickering.
            //
            // When items are loaded fast for chartInterval change
            // it shows loading state for a too little period of time.
            // It looks like a flickering.
            // It is true for most cases. Updating UI with some delay resolves it.
            // Since it is true for the most cases here we set delay.
            delay(300)
            emitState()
        }

        service.updateChartInterval(chartInterval)
    }

    fun refresh() {
        loading = true
        emitState()

        service.refresh()
    }

    private fun syncChartItems(chartPointsWrapper: ChartPointsWrapper?) {
        if (chartPointsWrapper == null || chartPointsWrapper.items.isEmpty()) {
            chartHeaderView = null
            chartInfoData = null

            return
        }

        val chartData = ChartData(chartPointsWrapper.items, chartPointsWrapper.isMovementChart, false)

        val headerView = if (!chartPointsWrapper.isMovementChart) {
            val value = valueFormatter.formatValue(service.currency, chartData.sum())
            ChartModule.ChartHeaderView(
                value = value,
                valueHint = null,
                date = null,
                diff = null,
                extraData = null
            )
        } else {
            val chartItems = chartPointsWrapper.items

            val latestItem = chartItems.last()
            val lastItemValue = latestItem.value
            val currentValue = valueFormatter.formatValue(service.currency, lastItemValue.toBigDecimal())

            val dominanceData = latestItem.dominance?.let { dominance ->
                val earliestItem = chartItems.first()
                val diff = earliestItem.dominance?.let { earliestDominance ->
                    Value.Percent((dominance - earliestDominance).toBigDecimal())
                }

                ChartModule.ChartHeaderExtraData.Dominance(
                    App.numberFormatter.format(dominance, 0, 2, suffix = "%"),
                    diff
                )
            }
            ChartModule.ChartHeaderView(
                value = currentValue,
                valueHint = null,
                date = null,
                diff = Value.Percent(chartData.diff()),
                extraData = dominanceData
            )
        }

        val (minValue, maxValue) = getMinMax(chartData.valueRange)

        val chartInfoData = ChartInfoData(
            chartData,
            maxValue,
            minValue
        )

        this.chartHeaderView = headerView
        this.chartInfoData = chartInfoData
    }

    private val noChangesLimitPercent = 0.2f
    private fun getMinMax(range: Range<Float>): Pair<String?, String?> {
        var max = range.upper
        var min = range.lower

        if (max!= null && min != null && max == min){
            min *= (1 - noChangesLimitPercent)
            max *= (1 + noChangesLimitPercent)
        }

        val maxValue = max?.let { getFormattedValue(it, service.currency) }
        val minValue = min?.let { getFormattedValue(it, service.currency) }

        return Pair(minValue, maxValue)

    }

    private fun getFormattedValue(value: Float, currency: Currency): String {
        return valueFormatter.formatValue(currency,  value.toBigDecimal())
    }

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }

    fun getSelectedPoint(item: ChartPoint): ChartModule.ChartHeaderView {
        val value = valueFormatter.formatValue(service.currency, item.value.toBigDecimal())
        val dayAndTime = DateHelper.getFullDate(Date(item.timestamp * 1000))

        return ChartModule.ChartHeaderView(
            value = value,
            valueHint = null,
            date = dayAndTime,
            diff = null,
            extraData = getItemExtraData(item)
        )
    }

    private fun getItemExtraData(item: ChartPoint): ChartModule.ChartHeaderExtraData? {
        val dominance = item.dominance
        val volume = item.volume

        return when {
            dominance != null -> {
                ChartModule.ChartHeaderExtraData.Dominance(
                    App.numberFormatter.format(dominance, 0, 2, suffix = "%"),
                    null
                )
            }
            volume != null -> ChartModule.ChartHeaderExtraData.Volume(
                App.numberFormatter.formatFiatShort(volume.toBigDecimal(), service.currency.symbol, 2)
            )
            else -> null
        }
    }
}

val HsTimePeriod.stringResId: Int
    get() = when (this) {
        HsTimePeriod.Day1 -> R.string.CoinPage_TimeDuration_Day
        HsTimePeriod.Week1 -> R.string.CoinPage_TimeDuration_Week
        HsTimePeriod.Week2 -> R.string.CoinPage_TimeDuration_TwoWeeks
        HsTimePeriod.Month1 -> R.string.CoinPage_TimeDuration_Month
        HsTimePeriod.Month3 -> R.string.CoinPage_TimeDuration_Month3
        HsTimePeriod.Month6 -> R.string.CoinPage_TimeDuration_HalfYear
        HsTimePeriod.Year1 -> R.string.CoinPage_TimeDuration_Year
        HsTimePeriod.Year2 -> R.string.CoinPage_TimeDuration_Year2
    }
