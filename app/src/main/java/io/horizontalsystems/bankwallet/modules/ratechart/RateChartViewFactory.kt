package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.core.NoRateStats
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.RateStatData
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import java.math.BigDecimal
import kotlin.math.max
import kotlin.math.min

data class ChartViewItem(
        val type: ChartType,
        val rateValue: CurrencyValue?,
        val marketCap: CurrencyValue,
        val lowValue: CurrencyValue,
        val highValue: CurrencyValue,
        val diffValue: BigDecimal,
        val chartData: ChartData
)

class RateChartViewFactory {
    fun createViewItem(chartType: ChartType, statData: RateStatData?, rate: Rate?, currency: Currency): ChartViewItem {
        val stat = statData ?: throw NoRateStats()
        val data = stat.stats[chartType.name] ?: throw NoRateStats()

        val statePoints = when (chartType) {
            ChartType.MONTHLY18 -> data.rates.takeLast(53) // for one year
            else -> data.rates
        }

        val points = statePoints.toMutableList()
        if (rate != null) {
            points.add(rate.value.toFloat())
        }

        val (min, max) = pointsEdges(points)

        val lowValue = CurrencyValue(currency, min)
        val highValue = CurrencyValue(currency, max)
        val marketCap = CurrencyValue(currency, stat.marketCap)
        val diffValue = diffInPercent(points.first { it != 0f }, points.last())
        val rateValue = rate?.let { CurrencyValue(currency, it.value) }
        val chartData = ChartData(points, data.timestamp, data.scale, chartType)

        return ChartViewItem(chartType, rateValue, marketCap, lowValue, highValue, diffValue, chartData)
    }

    private fun pointsEdges(points: List<Float>): Pair<BigDecimal, BigDecimal> {
        var min = Float.MIN_VALUE
        var max = Float.MIN_VALUE

        points.forEach {
            min = min(it, min)
            max = max(it, max)
        }

        return Pair(min.toBigDecimal(), max.toBigDecimal())
    }

    companion object {
        fun diffInPercent(pointStart: Float, pointEnd: Float): BigDecimal {
            val delta = -(pointStart - pointEnd)
            return (delta / pointStart * 100).toBigDecimal()
        }
    }
}
