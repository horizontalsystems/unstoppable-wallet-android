package io.horizontalsystems.bankwallet.modules.coin

import androidx.annotation.StringRes
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartDataFactory
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.chartview.models.MacdInfo
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.xrateskit.entities.*
import java.lang.Long.max
import java.math.BigDecimal
import java.math.RoundingMode

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
        val rateDiffs: Map<TimePeriod, Map<String, BigDecimal>>,
        val guideUrl: String?
)

class MarketData(@StringRes val title: Int, val value: String)

data class LastPoint(
        val rate: BigDecimal,
        val timestamp: Long,
        val rateDiff24h: BigDecimal
)

class CoinViewFactory(private val currency: Currency, private val numberFormatter: IAppNumberFormatter) {
    fun createChartInfo(type: ChartType, chartInfo: ChartInfo, lastPoint: LastPoint?): ChartInfoViewItem {
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
        val chartData = createChartData(chartInfo, lastPoint, chartType)

        return ChartInfoViewItem(chartData, chartType, chartData.diff())
    }

    fun createCoinDetailsViewItem(coinMarket: CoinMarketDetails, currency: Currency, coinCode: String, guideUrl: String? = null): CoinDetailsViewItem {
        return CoinDetailsViewItem(
                currency = currency,
                rateValue = coinMarket.rate,
                marketDataList = getMarketData(coinMarket, currency, coinCode),
                rateHigh24h = coinMarket.rateHigh24h,
                rateLow24h = coinMarket.rateLow24h,
                marketCapDiff24h = coinMarket.marketCapDiff24h,
                coinMeta = coinMarket.meta,
                rateDiffs = coinMarket.rateDiffs,
                guideUrl = guideUrl
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
            val (shortenValue, suffix) = numberFormatter.shortenValue(coinMarket.circulatingSupply)
            val value = "$shortenValue $suffix $coinCode"
            marketData.add(MarketData(R.string.CoinPage_inCirculation, value))
        }
        if (coinMarket.totalSupply > BigDecimal.ZERO) {
            val (shortenValue, suffix) = numberFormatter.shortenValue(coinMarket.totalSupply)
            val value = "$shortenValue $suffix $coinCode"
            marketData.add(MarketData(R.string.CoinPage_TotalSupply, value))
        }
        if (coinMarket.marketCap > BigDecimal.ZERO && coinMarket.circulatingSupply > BigDecimal.ZERO && coinMarket.totalSupply > BigDecimal.ZERO) {
            val rate = coinMarket.marketCap.divide(coinMarket.circulatingSupply, SCALE_UP_TO_BILLIONTH, RoundingMode.HALF_EVEN)
            val dilutedMarketCap = coinMarket.totalSupply.multiply(rate)
            marketData.add(MarketData(R.string.CoinPage_DilutedMarketCap, formatFiatShortened(dilutedMarketCap, currency.symbol)))
        }
        return marketData
    }

    private fun createChartData(chartInfo: ChartInfo, lastPoint: LastPoint?, chartType: ChartView.ChartType): ChartData {
        val points = chartInfo.points.map { ChartPoint(it.value.toFloat(), it.volume?.toFloat(), it.timestamp) }.toMutableList()
        val chartInfoLastPoint = chartInfo.points.lastOrNull()
        var endTimestamp = chartInfo.endTimestamp

        if (lastPoint != null && chartInfoLastPoint?.timestamp != null && lastPoint.timestamp > chartInfoLastPoint.timestamp) {
            endTimestamp = max(lastPoint.timestamp, endTimestamp)
            points.add(ChartPoint(lastPoint.rate.toFloat(), null, lastPoint.timestamp))

            if (chartType == ChartView.ChartType.DAILY) {
                val startTimestamp = lastPoint.timestamp - 24 * 60 * 60
                val startValue = lastPoint.rate.multiply(100.toBigDecimal()) / lastPoint.rateDiff24h.add(100.toBigDecimal())
                val startPoint = ChartPoint(startValue.toFloat(), null, startTimestamp)

                points.removeIf { it.timestamp <= startTimestamp }
                points.add(0, startPoint)
            }
        }

        return ChartDataFactory.build(points, chartInfo.startTimestamp, endTimestamp, chartInfo.isExpired)
    }

    fun createCoinMarketItems(tickers: List<MarketTicker>): List<MarketTickerViewItem> {
        return tickers.map {
            val (shortenValue, suffix) = numberFormatter.shortenValue(it.volume)
            MarketTickerViewItem(
                    it.marketName,
                    "${it.base}/${it.target}",
                    numberFormatter.formatCoin(it.rate, it.target, 0, 8),
                    "$shortenValue $suffix ${it.base}"
            )
        }
    }

    fun createCoinInvestorItems(fundCategories: List<CoinFundCategory>): List<InvestorItem> {
        val items = mutableListOf<InvestorItem>()
        fundCategories.forEach { category ->
            items.add(InvestorItem.Header(category.name))
            category.funds.forEachIndexed { index, fund ->
                items.add(InvestorItem.Fund(fund.name, fund.url, TextHelper.getCleanedUrl(fund.url), ListPosition.getListPosition(category.funds.size, index)))
            }
        }
        return items
    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        val shortCapValue = numberFormatter.shortenValue(value)
        return numberFormatter.formatFiat(shortCapValue.first, symbol, 0, 2) + " " + shortCapValue.second
    }

    companion object {
        const val SCALE_UP_TO_BILLIONTH = 9
    }

}
