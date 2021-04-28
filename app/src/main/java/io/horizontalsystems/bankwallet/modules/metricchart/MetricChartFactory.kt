package io.horizontalsystems.bankwallet.modules.metricchart

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartDataFactory
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import java.math.BigDecimal
import java.util.*

class MetricChartFactory(private val numberFormatter: IAppNumberFormatter) {

    fun convert(
            items: List<MetricChartModule.Item>,
            chartType: ChartView.ChartType,
            valueType: MetricChartModule.ValueType,
            currency: Currency
    ): ChartViewItem {

        val chartData = chartData(items)

        val maxValue = chartData.valueRange.upper?.let { getFormattedValue(it, currency, valueType) }
        val minValue = chartData.valueRange.lower?.let { getFormattedValue(it, currency, valueType) }

        val topValue = getFormattedValue(items.last().value.toFloat(), currency, valueType)
        val topValueWithDiff = LastValueWithDiff(topValue, chartData.diff())

        return ChartViewItem(topValueWithDiff, chartData, maxValue, minValue, chartType)
    }

    private fun chartData(points: List<MetricChartModule.Item>) : ChartData {
        val startTimestamp = points.first().timestamp
        val endTimestamp = points.last().timestamp
        val metricPoints = points.map { ChartPoint(it.value.toFloat(), null, it.timestamp) }
        return ChartDataFactory.build(metricPoints, startTimestamp, endTimestamp, false)
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
