package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.lib.chartview.ChartView
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.math.BigDecimal

data class ChartViewItem(
        val chartType: ChartView.ChartType,
        val chartPoints: List<ChartPoint>,
        val rateValue: CurrencyValue?,
        val marketCap: CurrencyValue,
        val lowValue: CurrencyValue,
        val highValue: CurrencyValue,
        val diffValue: BigDecimal,
        val lastUpdateTimestamp: Long,
        val startTimestamp: Long,
        val endTimestamp: Long
)

class RateChartViewFactory {
    fun createViewItem(type: ChartType, chartInfo: ChartInfo, marketInfo: MarketInfo, currency: Currency): ChartViewItem {
        val chartPoints = chartInfo.points.map { ChartPoint(it.value.toFloat(), it.timestamp) }

        val minValue = chartPoints.minBy { it.value }?.value ?: 0f
        val maxValue = chartPoints.maxBy { it.value }?.value ?: 0f

        val lowValue = CurrencyValue(currency, minValue.toBigDecimal())
        val highValue = CurrencyValue(currency, maxValue.toBigDecimal())
        val marketCap = CurrencyValue(currency, marketInfo.marketCap.toBigDecimal())
        val rateValue = CurrencyValue(currency, marketInfo.rate)

        val chartType = when (type) {
            ChartType.DAILY -> ChartView.ChartType.DAILY
            ChartType.WEEKLY -> ChartView.ChartType.WEEKLY
            ChartType.MONTHLY -> ChartView.ChartType.MONTHLY
            ChartType.MONTHLY6 -> ChartView.ChartType.MONTHLY6
            ChartType.MONTHLY12 -> ChartView.ChartType.MONTHLY18
        }

        return ChartViewItem(
                chartType,
                chartPoints,
                rateValue,
                marketCap,
                lowValue,
                highValue,
                marketInfo.diff,
                marketInfo.timestamp,
                chartInfo.startTimestamp,
                chartInfo.endTimestamp
        )
    }
}
