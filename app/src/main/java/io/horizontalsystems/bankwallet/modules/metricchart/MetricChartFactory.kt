package io.horizontalsystems.bankwallet.modules.metricchart

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.modules.chart.ChartDataXxx
import io.horizontalsystems.chartview.*
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import java.math.BigDecimal
import java.util.*

class MetricChartFactory(private val numberFormatter: IAppNumberFormatter) {

    private val noChangesLimitPercent = 0.2f

    fun convert(
        valueType: MetricChartModule.ValueType,
        currency: Currency,
        chartDataXxx: ChartDataXxx
    ): ChartViewItem {

        val chartData = chartData(chartDataXxx)

        var max = chartData.valueRange.upper
        var min = chartData.valueRange.lower

        if (max!= null && min != null && max == min){
            min *= (1 - noChangesLimitPercent)
            max *= (1 + noChangesLimitPercent)
        }

        val maxValue = max?.let { getFormattedValue(it, currency, valueType) }
        val minValue = min?.let { getFormattedValue(it, currency, valueType) }

        return ChartViewItem(chartData, maxValue, minValue, chartDataXxx.chartType)
    }

    private fun chartData(chartDataXxx: ChartDataXxx) : ChartData {
        val startTimestamp = chartDataXxx.startTimestamp
        val endTimestamp = chartDataXxx.endTimestamp
        val items = mutableListOf<ChartDataItem>()

        chartDataXxx.items.forEach { point ->
            val item = ChartDataItem(point.timestamp)
            item.values[Indicator.Candle] = ChartDataValue(point.value.toFloat())
            point.dominance?.let {
                item.values[Indicator.Dominance] = ChartDataValue(it.toFloat())
            }

            items.add(item)
        }

        val visibleChartData = ChartDataBuilder(mutableListOf(), startTimestamp, endTimestamp, chartDataXxx.isExpired)

        for (item in items) {
            visibleChartData.items.add(item)

            visibleChartData.range(item, Indicator.Candle)
            visibleChartData.range(item, Indicator.Dominance)
        }

        val visibleTimeInterval = visibleChartData.endTimestamp - visibleChartData.startTimestamp
        for (item in visibleChartData.items) {
            val timestamp = item.timestamp - visibleChartData.startTimestamp

            val x = (timestamp.toFloat() / visibleTimeInterval)

            item.setPoint(x, Indicator.Candle, visibleChartData.valueRange)
            item.setPoint(x, Indicator.Dominance, visibleChartData.dominanceRange)
        }

        return visibleChartData.build()
    }

    private fun getFormattedValue(value: Float, currency: Currency, valueType: MetricChartModule.ValueType): String {
        return when(valueType){
            MetricChartModule.ValueType.Percent -> numberFormatter.format(value, 0, 2, suffix = "%")
            MetricChartModule.ValueType.CurrencyValue -> numberFormatter.formatFiat(value, currency.symbol, 0, 2)
            MetricChartModule.ValueType.CompactCurrencyValue -> formatFiatShortened(value.toBigDecimal(), currency.symbol)
        }
    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        val (shortenValue, suffix) = numberFormatter.shortenValue(value)
        return numberFormatter.formatFiat(shortenValue, symbol, 0, 2) + " $suffix"
    }

    fun selectedPointViewItem(point: PointInfo, valueType: MetricChartModule.ValueType, currency: Currency): SelectedPoint? {
        val value: String = getFormattedValue(point.value, currency, valueType)
        val date = DateHelper.getDayAndTime(Date(point.timestamp * 1000))

        return SelectedPoint(value, date)
    }
}
