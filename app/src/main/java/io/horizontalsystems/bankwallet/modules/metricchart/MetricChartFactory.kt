package io.horizontalsystems.bankwallet.modules.metricchart

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.Indicator
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import java.math.BigDecimal
import java.util.*

class MetricChartFactory(private val numberFormatter: IAppNumberFormatter) {

    private val noChangesLimitPercent = 0.2f

    fun convert(
            items: List<MetricChartModule.Item>,
            chartType: ChartView.ChartType,
            valueType: MetricChartModule.ValueType,
            currency: Currency
    ): ChartViewItem {

        val chartData = chartData(items)

        var max = chartData.valueRange.upper
        var min = chartData.valueRange.lower

        if (max!= null && min != null && max == min){
            min *= (1 - noChangesLimitPercent)
            max *= (1 + noChangesLimitPercent)
        }

        val maxValue = max?.let { getFormattedValue(it, currency, valueType) }
        val minValue = min?.let { getFormattedValue(it, currency, valueType) }

        val topValue = getFormattedValue(items.last().value.toFloat(), currency, valueType)
        val topValueWithDiff = LastValueWithDiff(topValue, chartData.diff())

        return ChartViewItem(topValueWithDiff, chartData, maxValue, minValue, chartType)
    }

    private fun chartData(points: List<MetricChartModule.Item>) : ChartData {
        val startTimestamp = points.first().timestamp
        val endTimestamp = points.last().timestamp
        val items = mutableListOf<ChartData.Item>()

        points.forEach { point ->
            val item = ChartData.Item(point.timestamp)
            item.values[Indicator.Candle] = ChartData.Value(point.value.toFloat())
            point.dominance?.let {
                item.values[Indicator.Dominance] = ChartData.Value(it.toFloat())
            }

            items.add(item)
        }

        val visibleChartData = ChartData(mutableListOf(), startTimestamp, endTimestamp)

        for (item in items) {
            if (item.timestamp < visibleChartData.startTimestamp) {
                continue
            }

            visibleChartData.items.add(item)

            visibleChartData.range(item, Indicator.Candle)
            visibleChartData.range(item, Indicator.Dominance)
        }

        val visibleTimeInterval = visibleChartData.endTimestamp - visibleChartData.startTimestamp
        for (item in visibleChartData.items) {
            val timestamp = item.timestamp - visibleChartData.startTimestamp
            if (timestamp < 0) {
                continue
            }

            val x = (timestamp.toFloat() / visibleTimeInterval)

            item.setPoint(x, Indicator.Candle, visibleChartData.valueRange)
            item.setPoint(x, Indicator.Dominance, visibleChartData.dominanceRange)
        }

        return visibleChartData
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
