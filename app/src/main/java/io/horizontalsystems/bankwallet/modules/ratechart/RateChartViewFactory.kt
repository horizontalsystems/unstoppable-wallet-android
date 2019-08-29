package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.core.NoRateStats
import io.horizontalsystems.bankwallet.core.managers.StatsData
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
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
    fun createViewItem(chartType: ChartType, statData: StatsData, rate: Rate?, currency: Currency): ChartViewItem {
        val data = statData.stats[chartType.name] ?: throw NoRateStats()
        val diff = statData.diff[chartType.name] ?: throw NoRateStats()

        val min = data.points.min() ?: 0f
        val max = data.points.max() ?: 0f

        val lowValue = CurrencyValue(currency, min.toBigDecimal())
        val highValue = CurrencyValue(currency, max.toBigDecimal())
        val marketCap = CurrencyValue(currency, statData.marketCap)
        val rateValue = rate?.let { CurrencyValue(currency, it.value) }
        val chartData = ChartData(data.points, data.timestamp, data.scale, chartType)

        return ChartViewItem(
                chartType,
                rateValue,
                marketCap,
                lowValue,
                highValue,
                diff,
                chartData
        )
    }
}
