package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.core.NoRateStats
import io.horizontalsystems.bankwallet.core.managers.StatsData
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint
import java.math.BigDecimal

data class ChartViewItem(
        val type: ChartType,
        val rateValue: CurrencyValue?,
        val marketCap: CurrencyValue,
        val lowValue: CurrencyValue,
        val highValue: CurrencyValue,
        val diffValue: BigDecimal,
        val chartData: List<ChartPoint>,
        val lastUpdateTimestamp: Long? = null
)

class RateChartViewFactory {
    fun createViewItem(chartType: ChartType, statData: StatsData, rate: Rate?, currency: Currency): ChartViewItem {
        val diff = statData.diff[chartType.name] ?: throw NoRateStats()
        val points = statData.stats[chartType.name] ?: throw NoRateStats()

        val minValue = points.minBy { it.value }?.value ?: 0f
        val maxValue = points.maxBy { it.value }?.value ?: 0f

        val lowValue = CurrencyValue(currency, minValue.toBigDecimal())
        val highValue = CurrencyValue(currency, maxValue.toBigDecimal())
        val marketCap = CurrencyValue(currency, statData.marketCap)
        val rateValue = rate?.let { CurrencyValue(currency, it.value) }

        return ChartViewItem(
                chartType,
                rateValue,
                marketCap,
                lowValue,
                highValue,
                diff,
                points,
                rate?.timestamp?.times(1000)
        )
    }
}
