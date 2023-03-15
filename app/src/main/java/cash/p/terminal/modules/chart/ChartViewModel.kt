package cash.p.terminal.modules.chart

import android.util.Range
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.Currency
import cash.p.terminal.entities.ViewState
import cash.p.terminal.entities.viewState
import cash.p.terminal.modules.coin.ChartInfoData
import cash.p.terminal.modules.market.Value
import cash.p.terminal.ui.compose.components.TabItem
import io.horizontalsystems.chartview.ChartDataBuilder
import io.horizontalsystems.chartview.ChartDataItemImmutable
import io.horizontalsystems.chartview.Indicator
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

open class ChartViewModel(
    private val service: AbstractChartService,
    private val valueFormatter: ChartModule.ChartNumberFormatter
) : ViewModel() {
    val tabItemsLiveData = MutableLiveData<List<TabItem<HsTimePeriod?>>>()
    val dataWrapperLiveData = MutableLiveData<ChartDataWrapper>()
    val loadingLiveData = MutableLiveData<Boolean>()
    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)

    private val disposables = CompositeDisposable()

    init {
        loadingLiveData.postValue(true)

        service.chartTypeObservable
            .subscribeIO { chartType ->
                val tabItems = service.chartIntervals.map {
                    val titleResId = it?.stringResId ?: R.string.CoinPage_TimeDuration_All
                    TabItem(Translator.getString(titleResId), it == chartType.orElse(null), it)
                }
                tabItemsLiveData.postValue(tabItems)
            }
            .let {
                disposables.add(it)
            }

        service.chartPointsWrapperObservable
            .subscribeIO { chartItemsDataState ->
                chartItemsDataState.viewState?.let {
                    viewStateLiveData.postValue(it)
                }

                loadingLiveData.postValue(false)

                chartItemsDataState.getOrNull()?.let {
                    syncChartItems(it)
                }
            }
            .let {
                disposables.add(it)
            }

        viewModelScope.launch(Dispatchers.IO) {
            service.start()
        }
    }

    fun onSelectChartInterval(chartInterval: HsTimePeriod?) {
        loadingLiveData.postValue(true)
        service.updateChartInterval(chartInterval)
    }

    fun refresh() {
        loadingLiveData.postValue(true)
        service.refresh()
    }

    private fun syncChartItems(chartPointsWrapper: ChartPointsWrapper) {
        val chartItems = chartPointsWrapper.items
        if (chartItems.isEmpty()) return

        val chartData = ChartDataBuilder.buildFromPoints(
            chartPointsWrapper.items,
            chartPointsWrapper.startTimestamp,
            chartPointsWrapper.endTimestamp,
            chartPointsWrapper.isMovementChart,
            chartPointsWrapper.isExpired
        )

        val headerView = if (!chartPointsWrapper.isMovementChart) {
            val sum = valueFormatter.formatValue(service.currency, chartData.sum())
            ChartModule.ChartHeaderView.Sum(sum)
        } else {
            val lastItemValue = chartItems.last().value
            val currentValue = valueFormatter.formatValue(service.currency, lastItemValue.toBigDecimal())
            ChartModule.ChartHeaderView.Latest(currentValue, Value.Percent(chartData.diff()))
        }

        val (minValue, maxValue) = getMinMax(chartData.valueRange)

        val chartInfoData = ChartInfoData(
            chartData,
            chartPointsWrapper.chartInterval,
            maxValue,
            minValue,
            chartData.items.any { it.values[Indicator.Volume] != null }
        )

        dataWrapperLiveData.postValue(ChartDataWrapper(headerView, chartInfoData))
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

    fun getSelectedPoint(item: ChartDataItemImmutable): SelectedPoint? {
        return item.values[Indicator.Candle]?.let { candle ->
            val value = valueFormatter.formatValue(service.currency, candle.toBigDecimal())
            val dayAndTime = DateHelper.getFullDate(Date(item.timestamp * 1000))

            SelectedPoint(
                value = value,
                date = dayAndTime,
                extraData = getItemExtraData(item),
            )
        }
    }

    private fun getItemExtraData(item: ChartDataItemImmutable): SelectedPoint.ExtraData? {
        val dominance = item.values[Indicator.Dominance]
        val volume = item.values[Indicator.Volume]

        return when {
            dominance != null -> SelectedPoint.ExtraData.Dominance(
                App.numberFormatter.format(dominance, 0, 2, suffix = "%")
            )
            volume != null -> SelectedPoint.ExtraData.Volume(
                App.numberFormatter.formatFiatShort(volume.toBigDecimal(), service.currency.symbol, 2)
            )
            else -> null
        }
    }
}

data class SelectedPoint(
    val value: String,
    val date: String,
    val extraData: ExtraData?
) {
    sealed class ExtraData {
        class Volume(val volume: String) : ExtraData()
        class Dominance(val dominance: String) : ExtraData()
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
