package io.horizontalsystems.bankwallet.modules.coin.overview

import io.horizontalsystems.bankwallet.core.IChartTypeStorage
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartPointsWrapper
import io.horizontalsystems.chartview.Indicator
import io.horizontalsystems.chartview.helpers.IndicatorHelper
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.ChartInfo
import io.horizontalsystems.marketkit.models.ChartPointType
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable

class CoinOverviewChartService(
    private val marketKit: MarketKit,
    override val currencyManager: ICurrencyManager,
    private val chartTypeStorage: IChartTypeStorage,
    private val coinUid: String,
) : AbstractChartService() {

    override val initialChartInterval by chartTypeStorage::chartInterval

    override val chartIntervals = HsTimePeriod.values().toList()

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

    override fun updateChartInterval(chartInterval: HsTimePeriod) {
        super.updateChartInterval(chartInterval)
        chartTypeStorage.chartInterval = chartInterval
    }

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency,
    ): Single<ChartPointsWrapper> {
        val newKey = chartInterval.name + currency.code
        if (newKey != updatesSubscriptionKey) {
            unsubscribeFromUpdates()
            subscribeForUpdates(currency, chartInterval)
            updatesSubscriptionKey = newKey
        }

        val tmpChartInfo: ChartInfo? = marketKit.chartInfo(coinUid, currency.code, chartInterval)
        val tmpLastCoinPrice = marketKit.coinPrice(coinUid, currency.code)

        return Single.just(doGetItems(tmpChartInfo, tmpLastCoinPrice, chartInterval))
    }

    private fun unsubscribeFromUpdates() {
        disposables.clear()
    }

    private fun subscribeForUpdates(currency: Currency, chartInterval: HsTimePeriod) {
        marketKit.coinPriceObservable(coinUid, currency.code)
            .subscribeIO {
                dataInvalidated()
            }
            .let {
                disposables.add(it)
            }

        marketKit.getChartInfoAsync(coinUid, currency.code, chartInterval)
            .subscribeIO {
                dataInvalidated()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun doGetItems(
        chartInfo: ChartInfo?,
        lastCoinPrice: CoinPrice?,
        chartInterval: HsTimePeriod
    ): ChartPointsWrapper {
        if (chartInfo == null || lastCoinPrice == null) return ChartPointsWrapper(chartInterval, listOf())
        val points = chartInfo.points
        if (points.isEmpty()) return ChartPointsWrapper(chartInterval, listOf())

        val values = points.map { it.value.toFloat() }
        val emaFast = IndicatorHelper.ema(values, Indicator.EmaFast.period)
        val emaSlow = IndicatorHelper.ema(values, Indicator.EmaSlow.period)

        val rsi = IndicatorHelper.rsi(values, Indicator.Rsi.period)
        val (macd, signal, histogram) = IndicatorHelper.macd(values, Indicator.Macd.fastPeriod, Indicator.Macd.slowPeriod, Indicator.Macd.signalPeriod)

        val items = points
            .mapIndexed { index, chartPoint ->
                val indicators = mapOf(
                    Indicator.Volume to chartPoint.extra[ChartPointType.Volume]?.toFloat(),
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
                    indicators = indicators
                )
            }
            .toMutableList()

        if (lastCoinPrice.timestamp > items.last().timestamp) {
            items.add(ChartPoint(lastCoinPrice.value.toFloat(), timestamp = lastCoinPrice.timestamp))

            if (chartInterval == HsTimePeriod.Day1) {
                val startTimestamp = lastCoinPrice.timestamp - 24 * 60 * 60
                val startValue = (lastCoinPrice.value * 100.toBigDecimal()) / (lastCoinPrice.diff + 100.toBigDecimal())
                val startItem = ChartPoint(startValue.toFloat(), startTimestamp)

                items.removeIf { it.timestamp <= startTimestamp }
                items.add(0, startItem)
            }
        }

        items.removeIf { it.timestamp < chartInfo.startTimestamp }

        return ChartPointsWrapper(chartInterval, items, chartInfo.startTimestamp, chartInfo.endTimestamp, chartInfo.isExpired)
    }

}
