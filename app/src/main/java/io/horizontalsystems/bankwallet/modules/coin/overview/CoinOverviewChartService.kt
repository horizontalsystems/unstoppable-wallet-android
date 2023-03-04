package io.horizontalsystems.bankwallet.modules.coin.overview

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartPointsWrapper
import io.horizontalsystems.chartview.Indicator
import io.horizontalsystems.chartview.helpers.IndicatorHelper
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.marketkit.models.*
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.rx2.await

class CoinOverviewChartService(
    private val marketKit: MarketKitWrapper,
    override val currencyManager: CurrencyManager,
    private val coinUid: String,
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Day1

    override var chartIntervals = listOf<HsTimePeriod?>()

    override val chartIndicators = listOf(
        ChartIndicator.Ema,
        ChartIndicator.Macd,
        ChartIndicator.Rsi
    )

    private var updatesSubscriptionKey: String? = null
    private val disposables = CompositeDisposable()

    private var chartStartTime: Long = 0

    override suspend fun start() {
        chartStartTime = marketKit.chartStartTimeSingle(coinUid).await()
        val now = System.currentTimeMillis() / 1000L
        val mostPeriodSeconds = now - chartStartTime

        chartIntervals = HsTimePeriod.values().filter {
            it.range <= mostPeriodSeconds
        } + listOf<HsTimePeriod?>(null)

        super.start()
    }

    override fun stop() {
        super.stop()
        unsubscribeFromUpdates()
    }

    override fun getAllItems(currency: Currency): Single<ChartPointsWrapper> {
        return getItemsByPeriodType(
            currency = currency,
            periodType = HsPeriodType.ByStartTime(chartStartTime),
            chartInterval = null
        )
    }

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency,
    ): Single<ChartPointsWrapper> {
        return getItemsByPeriodType(
            currency = currency,
            periodType = HsPeriodType.ByPeriod(chartInterval),
            chartInterval = chartInterval
        )
    }

    private fun getItemsByPeriodType(
        currency: Currency,
        periodType: HsPeriodType,
        chartInterval: HsTimePeriod?
    ): Single<ChartPointsWrapper> {
        val newKey = (chartInterval?.name ?: "All") + currency.code
        if (forceRefresh || newKey != updatesSubscriptionKey) {
            unsubscribeFromUpdates()
            subscribeForUpdates(currency, periodType)
            updatesSubscriptionKey = newKey
        }

        val tmpChartInfo = marketKit.chartInfo(coinUid, currency.code, periodType)
        val tmpLastCoinPrice = marketKit.coinPrice(coinUid, currency.code)

        return Single.just(doGetItems(tmpChartInfo, tmpLastCoinPrice, chartInterval))
    }

    private fun unsubscribeFromUpdates() {
        disposables.clear()
    }

    private fun subscribeForUpdates(currency: Currency, periodType: HsPeriodType) {
        marketKit.coinPriceObservable(coinUid, currency.code)
            .subscribeIO {
                dataInvalidated()
            }
            .let {
                disposables.add(it)
            }

        marketKit.getChartInfoAsync(coinUid, currency.code, periodType)
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
        chartInterval: HsTimePeriod?
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
                val diff = lastCoinPrice.diff
                if (diff == null) {
                    items.removeIf { it.timestamp < startTimestamp }
                } else {
                    items.removeIf { it.timestamp <= startTimestamp }

                    val startValue = (lastCoinPrice.value * 100.toBigDecimal()) / (diff + 100.toBigDecimal())
                    val startItem = ChartPoint(startValue.toFloat(), startTimestamp)

                    items.add(0, startItem)
                }
            }
        }

        items.removeIf { it.timestamp < chartInfo.startTimestamp }

        return ChartPointsWrapper(chartInterval, items, chartInfo.startTimestamp, chartInfo.endTimestamp, chartInfo.isExpired)
    }

}
