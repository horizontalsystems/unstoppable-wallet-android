package io.horizontalsystems.bankwallet.modules.coin.overview

import android.util.Log
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorManager
import io.horizontalsystems.bankwallet.modules.chart.ChartPointsWrapper
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.marketkit.models.HsPeriodType
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import retrofit2.HttpException
import java.io.IOException
import io.horizontalsystems.marketkit.models.ChartPoint as MarketKitChartPoint

class CoinOverviewChartService(
    private val marketKit: MarketKitWrapper,
    override val currencyManager: CurrencyManager,
    private val coinUid: String,
    private val chartIndicatorManager: ChartIndicatorManager,
) : AbstractChartService() {
    override val hasVolumes = true
    override val initialChartInterval = HsTimePeriod.Day1

    override var chartIntervals = listOf<HsTimePeriod?>()
    override val chartViewType = ChartViewType.Line

    private var updatesSubscriptionKey: String? = null
    private val disposables = CompositeDisposable()

    private var chartStartTime: Long = 0
    private val cache = mutableMapOf<String, Pair<Long, List<MarketKitChartPoint>>>()

    private val scope = CoroutineScope(Dispatchers.IO)

    private var indicatorsEnabled = chartIndicatorManager.isEnabledFlow.value

    override suspend fun start() {
        try {
            chartStartTime = marketKit.chartStartTimeSingle(coinUid).await()
        } catch (e: IOException) {
            Log.e("CoinOverviewChartService", "start error: ", e)
        } catch (e: HttpException) {
            Log.e("CoinOverviewChartService", "start error: ", e)
        }

        val now = System.currentTimeMillis() / 1000L
        val mostPeriodSeconds = now - chartStartTime

        chartIntervals = HsTimePeriod.values().filter {
            it.range <= mostPeriodSeconds
        } + listOf<HsTimePeriod?>(null)

        scope.launch {
            chartIndicatorManager.isEnabledFlow.collect {
                indicatorsEnabled = it
                dataInvalidated()
            }
        }
        scope.launch {
            chartIndicatorManager.allIndicatorsFlow.collect {
                if (indicatorsEnabled) {
                    dataInvalidated()
                }
            }
        }

        super.start()
    }

    override fun stop() {
        super.stop()
        unsubscribeFromUpdates()
        scope.cancel()
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
        val periodType = if (indicatorsEnabled) {
            HsPeriodType.ByCustomPoints(chartInterval, chartIndicatorManager.getPointsCount())
        } else {
            HsPeriodType.ByPeriod(chartInterval)
        }

        return getItemsByPeriodType(
            currency = currency,
            periodType = periodType,
            chartInterval = chartInterval
        )
    }

    private fun getItemsByPeriodType(
        currency: Currency,
        periodType: HsPeriodType,
        chartInterval: HsTimePeriod?
    ): Single<ChartPointsWrapper> {
        val newKey = currency.code
        if (newKey != updatesSubscriptionKey) {
            unsubscribeFromUpdates()
            subscribeForUpdates(currency)
            updatesSubscriptionKey = newKey
        }

        return chartInfoCached(currency, periodType)
            .map { (startTimestamp, points) ->
                doGetItems(startTimestamp, points, chartInterval)
            }
    }

    private fun chartInfoCached(
        currency: Currency,
        periodType: HsPeriodType
    ): Single<Pair<Long, List<MarketKitChartPoint>>> {
        val cacheKey = currency.code + periodType.serialize()
        val cached = cache[cacheKey]
        return if (cached != null) {
            Single.just(cached)
        } else {
            marketKit.chartPointsSingle(coinUid, currency.code, periodType)
                .doOnSuccess {
                    cache[cacheKey] = it
                }
        }
    }

    private fun unsubscribeFromUpdates() {
        disposables.clear()
    }

    private fun subscribeForUpdates(currency: Currency) {
        marketKit.coinPriceObservable("coin-overview-chart-service", coinUid, currency.code)
            .subscribeIO {
                dataInvalidated()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun doGetItems(
        startTimestamp: Long,
        points: List<MarketKitChartPoint>,
        chartInterval: HsTimePeriod?
    ): ChartPointsWrapper {
        if (points.isEmpty()) return ChartPointsWrapper(listOf())
        val latestCoinPrice = marketKit.coinPrice(coinUid, currency.code) ?: return ChartPointsWrapper(listOf())

        val pointsAdjusted = points.toMutableList()
        var startTimestampAdjusted = startTimestamp

        if (latestCoinPrice.timestamp > pointsAdjusted.last().timestamp) {
            pointsAdjusted.add(
                MarketKitChartPoint(latestCoinPrice.value, latestCoinPrice.timestamp, null)
            )

            if (chartInterval == HsTimePeriod.Day1) {
                startTimestampAdjusted = latestCoinPrice.timestamp - 24 * 60 * 60
                val diff = latestCoinPrice.diff
                if (diff != null) {
                    val startValue =
                        (latestCoinPrice.value * 100.toBigDecimal()) / (diff + 100.toBigDecimal())
                    val startPoint = MarketKitChartPoint(startValue, startTimestampAdjusted, null)
                    val indexOfLast =
                        pointsAdjusted.indexOfLast { it.timestamp < startTimestampAdjusted }
                    pointsAdjusted.add(indexOfLast + 1, startPoint)
                }
            }
        }

        val indicators = if (indicatorsEnabled) {
            val pointsForIndicators = LinkedHashMap(pointsAdjusted.associate { it.timestamp to it.value.toFloat() })
            chartIndicatorManager.calculateIndicators(pointsForIndicators, startTimestampAdjusted)
        } else {
            mapOf()
        }

        val items = pointsAdjusted
            .filter { it.timestamp >= startTimestampAdjusted}
            .map { chartPoint ->
                ChartPoint(
                    value = chartPoint.value.toFloat(),
                    timestamp = chartPoint.timestamp,
                    volume = chartPoint.volume?.toFloat(),
                )
            }

        return ChartPointsWrapper(items, indicators = indicators)
    }

}
