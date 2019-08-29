package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.core.NoRateStats
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.RateStatData
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import java.math.BigDecimal

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

        val min = points.min() ?: 0f
        val max = points.max() ?: 0f

        val lowValue = CurrencyValue(currency, min.toBigDecimal())
        val highValue = CurrencyValue(currency, max.toBigDecimal())
        val marketCap = CurrencyValue(currency, stat.marketCap)
        val diffValue = growthDiff(points)
        val rateValue = rate?.let { CurrencyValue(currency, it.value) }
        val chartData = ChartData(points, data.timestamp, data.scale, chartType)

        return ChartViewItem(chartType, rateValue, marketCap, lowValue, highValue, diffValue, chartData)
    }

    companion object {
        fun growthDiff(points: List<Float>): BigDecimal {
            val pointStart = points.first { it != 0f }
            val pointEnd = points.last()
            val delta = -(pointStart - pointEnd)

            return (delta / pointStart * 100).toBigDecimal()
        }
    }
}
