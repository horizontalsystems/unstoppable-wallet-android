package io.horizontalsystems.bankwallet.modules.coin

import androidx.annotation.StringRes
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartDataFactory
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.chartview.models.MacdInfo
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.*
import java.lang.Long.max
import java.math.BigDecimal

data class ChartInfoViewItem(
        val chartData: ChartData,
        val chartType: ChartView.ChartType,
        val diffValue: BigDecimal
)

data class ChartPointViewItem(
        val date: Long,
        val price: CurrencyValue,
        val volume: CurrencyValue?,
        val macdInfo: MacdInfo?
)

data class CoinDetailsViewItem(
        val currency: Currency,
        val rateValue: BigDecimal,
        val marketDataList: List<MarketData>,
        val rateHigh24h: BigDecimal,
        val rateLow24h: BigDecimal,
        val marketCapDiff24h: BigDecimal,
        val coinMeta: CoinMeta,
        val rateDiffs: Map<TimePeriod, Map<String, BigDecimal>>
)

class MarketData(@StringRes val title: Int, val value: String)

data class LastPoint(
        val rate: BigDecimal,
        val timestamp: Long
)

class CoinViewFactory(private val currency: Currency, private val numberFormatter: IAppNumberFormatter) {
    fun createChartInfo(type: ChartType, chartInfo: ChartInfo, lastPoint: LastPoint?): ChartInfoViewItem {
        val chartData = createChartData(chartInfo, lastPoint)
        val chartType = when (type) {
            ChartType.TODAY -> ChartView.ChartType.TODAY
            ChartType.DAILY -> ChartView.ChartType.DAILY
            ChartType.WEEKLY -> ChartView.ChartType.WEEKLY
            ChartType.WEEKLY2 -> ChartView.ChartType.WEEKLY2
            ChartType.MONTHLY -> ChartView.ChartType.MONTHLY
            ChartType.MONTHLY3 -> ChartView.ChartType.MONTHLY3
            ChartType.MONTHLY6 -> ChartView.ChartType.MONTHLY6
            ChartType.MONTHLY12 -> ChartView.ChartType.MONTHLY12
            ChartType.MONTHLY24 -> ChartView.ChartType.MONTHLY24
        }

        return ChartInfoViewItem(chartData, chartType, chartData.diff())
    }

    fun createCoinDetailsViewItem(coinMarket: CoinMarketDetails, currency: Currency, coinCode: String): CoinDetailsViewItem {
        return CoinDetailsViewItem(
                currency = currency,
                rateValue = coinMarket.rate,
                marketDataList = getMarketData(coinMarket, currency, coinCode),
                rateHigh24h = coinMarket.rateHigh24h,
                rateLow24h = coinMarket.rateLow24h,
                marketCapDiff24h = coinMarket.marketCapDiff24h,
                coinMeta = coinMarket.meta,
                rateDiffs = coinMarket.rateDiffs
        )
    }

    private fun getMarketData(coinMarket: CoinMarketDetails, currency: Currency, coinCode: String): MutableList<MarketData> {
        val marketData = mutableListOf<MarketData>()
        if (coinMarket.marketCap > BigDecimal.ZERO) {
            marketData.add(MarketData(R.string.CoinPage_MarketCap, formatFiatShortened(coinMarket.marketCap, currency.symbol)))
        }
        if (coinMarket.volume24h > BigDecimal.ZERO) {
            marketData.add(MarketData(R.string.CoinPage_Volume24, formatFiatShortened(coinMarket.volume24h, currency.symbol)))
        }
        if (coinMarket.circulatingSupply > BigDecimal.ZERO) {
            val value = numberFormatter.formatCoin(coinMarket.circulatingSupply, coinCode, 0, 2)
            marketData.add(MarketData(R.string.CoinPage_inCirculation, value))
        }
        if (coinMarket.totalSupply > BigDecimal.ZERO) {
            val value = numberFormatter.formatCoin(coinMarket.totalSupply, coinCode, 0, 2)
            marketData.add(MarketData(R.string.CoinPage_TotalSupply, value))
        }
        return marketData
    }

    private fun createChartData(chartInfo: ChartInfo, lastPoint: LastPoint?): ChartData {
        val points = chartInfo.points.map { ChartPoint(it.value.toFloat(), it.volume?.toFloat(), it.timestamp) }.toMutableList()
        val chartInfoLastPoint = chartInfo.points.lastOrNull()
        var endTimestamp = chartInfo.endTimestamp

        if (lastPoint != null && chartInfoLastPoint?.timestamp != null && lastPoint.timestamp > chartInfoLastPoint.timestamp) {
            endTimestamp = max(lastPoint.timestamp, endTimestamp)
            points.add(ChartPoint(lastPoint.rate.toFloat(), null, lastPoint.timestamp))
        }

        return ChartDataFactory.build(points, chartInfo.startTimestamp, endTimestamp, chartInfo.isExpired)
    }

    fun createCoinMarketItems(coinDetails: CoinMarketDetails): List<MarketTickerViewItem> {
        return coinDetails.tickers.map {
            val (shortenValue, suffix) = App.numberFormatter.shortenValue(it.volume)
            MarketTickerViewItem(
                    it.marketName,
                    "${it.base}/${it.target}",
                    numberFormatter.formatFiat(it.rate, currency.symbol, 0, 6),
                    numberFormatter.formatFiat(shortenValue, currency.symbol, 0, 2) + " $suffix ${it.target}"
            )
        }
    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        val shortCapValue = numberFormatter.shortenValue(value)
        return numberFormatter.formatFiat(shortCapValue.first, symbol, 0, 2) + " " + shortCapValue.second
    }
}
