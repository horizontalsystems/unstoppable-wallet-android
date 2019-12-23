package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.lib.chartview.ChartView
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.math.BigDecimal

data class ChartInfoViewItem(
        val chartType: ChartView.ChartType,
        val chartPoints: List<ChartPoint>,
        val diffValue: BigDecimal,
        val startTimestamp: Long,
        val endTimestamp: Long,
        val currency: Currency
)

data class MarketInfoViewItem(
        val rateValue: CurrencyValue,
        val marketCap: CurrencyValue,
        val volume: CurrencyValue,
        val supply: CoinValue,
        val maxSupply: CoinValue?,
        val timestamp: Long
)

class RateChartViewFactory {
    fun createChartInfo(type: ChartType, chartInfo: ChartInfo, currency: Currency): ChartInfoViewItem {
        val chartPoints = chartInfo.points.map { ChartPoint(it.value.toFloat(), it.timestamp) }

        val startValue = chartPoints.firstOrNull()?.value ?: 0f
        val endValue = chartPoints.lastOrNull()?.value ?: 0f

        val chartType = when (type) {
            ChartType.DAILY -> ChartView.ChartType.DAILY
            ChartType.WEEKLY -> ChartView.ChartType.WEEKLY
            ChartType.MONTHLY -> ChartView.ChartType.MONTHLY
            ChartType.MONTHLY6 -> ChartView.ChartType.MONTHLY6
            ChartType.MONTHLY12 -> ChartView.ChartType.MONTHLY12
        }

        val diffValue = ((endValue - startValue) / startValue * 100).toBigDecimal()

        return ChartInfoViewItem(
                chartType,
                chartPoints,
                diffValue,
                chartInfo.startTimestamp,
                chartInfo.endTimestamp,
                currency
        )
    }

    fun createMarketInfo(marketInfo: MarketInfo, currency: Currency, coin: Coin): MarketInfoViewItem {
        return MarketInfoViewItem(
                CurrencyValue(currency, marketInfo.rate),
                CurrencyValue(currency, marketInfo.marketCap.toBigDecimal()),
                CurrencyValue(currency, marketInfo.volume.toBigDecimal()),
                CoinValue(coin, marketInfo.supply.toBigDecimal()),
                MaxSupplyMap.maxSupplies[coin.code]?.let { CoinValue(coin, it) },
                marketInfo.timestamp
        )
    }
}
