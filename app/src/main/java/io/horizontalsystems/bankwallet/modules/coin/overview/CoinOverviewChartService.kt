package io.horizontalsystems.bankwallet.modules.coin.overview

import io.horizontalsystems.bankwallet.core.IChartTypeStorage
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartDataXxx
import io.horizontalsystems.bankwallet.modules.metricchart.kitChartType
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.Indicator
import io.horizontalsystems.chartview.helpers.IndicatorHelper
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.ChartInfo
import io.horizontalsystems.marketkit.models.CoinPrice
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable

class CoinOverviewChartService(
    private val marketKit: MarketKit,
    override val currencyManager: ICurrencyManager,
    private val chartTypeStorage: IChartTypeStorage,
    private val coinUid: String,
) : AbstractChartService() {

    override val initialChartType by chartTypeStorage::chartType2

    override val chartTypes = listOf(
        ChartView.ChartType.TODAY,
        ChartView.ChartType.DAILY,
        ChartView.ChartType.WEEKLY,
        ChartView.ChartType.WEEKLY2,
        ChartView.ChartType.MONTHLY,
        ChartView.ChartType.MONTHLY3,
        ChartView.ChartType.MONTHLY6,
        ChartView.ChartType.MONTHLY12,
        ChartView.ChartType.MONTHLY24
    )

    override val chartIndicators = listOf(
        ChartIndicator.Ema,
        ChartIndicator.Macd,
        ChartIndicator.Rsi
    )

    private var updatesSubscriptionKey: String? = null
    private val disposables = CompositeDisposable()

    override fun stop() {
        super.stop()
        unsubscribeFromUpdates()
    }

    override fun getItems(
        chartType: ChartView.ChartType,
        currency: Currency,
    ): Single<ChartDataXxx> {
        val newKey = chartType.name + currency.code
        if (newKey != updatesSubscriptionKey) {
            unsubscribeFromUpdates()
            subscribeForUpdates(currency, chartType)
            updatesSubscriptionKey = newKey
        }

        val tmpChartInfo = marketKit.chartInfo(coinUid, currency.code, chartType.kitChartType)
        val tmpLastCoinPrice = marketKit.coinPrice(coinUid, currency.code)

        val items = if (tmpChartInfo != null && tmpLastCoinPrice != null) {
            doGetItems(tmpChartInfo, tmpLastCoinPrice, chartType)
        } else {
            ChartDataXxx(chartType, listOf())
        }

        return Single.just(items)
    }

    private fun unsubscribeFromUpdates() {
        disposables.clear()
    }

    private fun subscribeForUpdates(currency: Currency, chartType: ChartView.ChartType) {
        marketKit.coinPriceObservable(coinUid, currency.code)
            .subscribeIO {
                dataInvalidated()
            }
            .let {
                disposables.add(it)
            }

        marketKit.getChartInfoAsync(coinUid, currency.code, chartType.kitChartType)
            .subscribeIO {
                dataInvalidated()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun doGetItems(
        chartInfo: ChartInfo,
        lastCoinPrice: CoinPrice,
        chartType: ChartView.ChartType
    ): ChartDataXxx {
        val points = chartInfo.points
        if (points.isEmpty()) return ChartDataXxx(chartType, listOf())

        val values = points.map { it.value.toFloat() }
        val emaFast = IndicatorHelper.emaXxx(values, Indicator.EmaFast.period)
        val emaSlow = IndicatorHelper.emaXxx(values, Indicator.EmaSlow.period)

        val rsi = IndicatorHelper.rsiXxx(values, Indicator.Rsi.period)
        val (macd, signal, histogram) = IndicatorHelper.macdXxx(values, Indicator.Macd.fastPeriod, Indicator.Macd.slowPeriod, Indicator.Macd.signalPeriod)

        val items = points
            .mapIndexed { index, chartPoint ->
                val indicators = mapOf(
                    Indicator.EmaFast to emaFast.getOrNull(index),
                    Indicator.EmaSlow to emaSlow.getOrNull(index),
                    Indicator.Rsi to rsi.getOrNull(index),
                    Indicator.Macd to macd.getOrNull(index),
                    Indicator.MacdSignal to signal.getOrNull(index),
                    Indicator.MacdHistogram to histogram.getOrNull(index),
                )

                ChartPoint(
                    value = chartPoint.value.toFloat(),
                    timestamp = chartPoint.timestamp,
                    volume = chartPoint.volume?.toFloat(),
                    indicators = indicators
                )
            }
            .toMutableList()

        if (lastCoinPrice.timestamp > items.last().timestamp) {
            items.add(ChartPoint(lastCoinPrice.value.toFloat(), timestamp = lastCoinPrice.timestamp))

            if (chartType == ChartView.ChartType.DAILY) {
                val startTimestamp = lastCoinPrice.timestamp - 24 * 60 * 60
                val startValue = (lastCoinPrice.value * 100.toBigDecimal()) / (lastCoinPrice.diff + 100.toBigDecimal())
                val startItem = ChartPoint(startValue.toFloat(), startTimestamp)

                items.removeIf { it.timestamp <= startTimestamp }
                items.add(0, startItem)
            }
        }

        items.removeIf { it.timestamp < chartInfo.startTimestamp }

        return ChartDataXxx(chartType, items, chartInfo.startTimestamp, chartInfo.endTimestamp, chartInfo.isExpired)
    }

}
