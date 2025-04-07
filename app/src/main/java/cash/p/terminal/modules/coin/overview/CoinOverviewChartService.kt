package cash.p.terminal.modules.coin.overview

import android.util.Log

import io.horizontalsystems.chartview.chart.AbstractChartService
import cash.p.terminal.modules.chart.ChartIndicatorManager
import io.horizontalsystems.chartview.chart.ChartPointsWrapper
import io.horizontalsystems.core.CurrencyManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.models.CoinPrice
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.models.HsPeriodType
import io.horizontalsystems.core.models.HsTimePeriod
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.models.ChartPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import retrofit2.HttpException
import java.io.IOException
import java.math.BigDecimal
import cash.p.terminal.wallet.models.ChartPoint as MarketKitChartPoint

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
    private var updatesJob: Job? = null

    private var chartStartTime: Long = 0
    private val cache = mutableMapOf<String, Pair<Long, List<MarketKitChartPoint>>>()

    private var indicatorsEnabled = chartIndicatorManager.isEnabled

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

        coroutineScope.launch {
            chartIndicatorManager.isEnabledFlow.collect {
                indicatorsEnabled = it
                dataInvalidated()
            }
        }
        coroutineScope.launch {
            chartIndicatorManager.allIndicatorsFlow.collect {
                if (indicatorsEnabled) {
                    dataInvalidated()
                }
            }
        }

        super.start()
    }

    override suspend fun getAllItems(currency: Currency): ChartPointsWrapper {
        return getItemsByPeriodType(
            currency = currency,
            periodType = HsPeriodType.ByStartTime(chartStartTime),
            chartInterval = null
        )
    }

    override suspend fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency,
    ): ChartPointsWrapper {
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

    override fun updateChartInterval(chartInterval: HsTimePeriod?) {
        super.updateChartInterval(chartInterval)
    }

    private suspend fun getItemsByPeriodType(
        currency: Currency,
        periodType: HsPeriodType,
        chartInterval: HsTimePeriod?
    ): ChartPointsWrapper {
        val newKey = currency.code
        if (newKey != updatesSubscriptionKey) {
            subscribeForUpdates(currency)
            updatesSubscriptionKey = newKey
        }

        return chartInfoCached(currency, periodType)
            .let { (startTimestamp, points) ->
                doGetItems(startTimestamp, points, chartInterval)
            }
    }

    private suspend fun chartInfoCached(
        currency: Currency,
        periodType: HsPeriodType
    ): Pair<Long, List<MarketKitChartPoint>> {
        val cacheKey = currency.code + periodType.serialize()
        return cache[cacheKey]
            ?: marketKit.chartPointsSingle(coinUid, currency.code, periodType)
                .also {
                    cache[cacheKey] = it
                }
    }

    private fun subscribeForUpdates(currency: Currency) {
        updatesJob?.cancel()
        updatesJob = coroutineScope.launch {
            marketKit.coinPriceObservable("coin-overview-chart-service", coinUid, currency.code).asFlow().collect {
                dataInvalidated()
            }
        }
    }

    private fun doGetItems(
        startTimestamp: Long,
        points: List<MarketKitChartPoint>,
        chartInterval: HsTimePeriod?
    ): ChartPointsWrapper {
        if (points.isEmpty()) return ChartPointsWrapper(listOf())
        val latestCoinPrice = marketKit.coinPrice(coinUid, currency.code) ?: CoinPrice(coinUid, currency.code, points.last().value, BigDecimal.ZERO, BigDecimal.ZERO, points.last().timestamp)

        val pointsAdjusted = points.toMutableList()
        var startTimestampAdjusted = startTimestamp

        if (latestCoinPrice.timestamp > pointsAdjusted.last().timestamp) {
            pointsAdjusted.add(
                MarketKitChartPoint(latestCoinPrice.value, latestCoinPrice.timestamp, null)
            )

            if (chartInterval == HsTimePeriod.Day1) {
                startTimestampAdjusted = latestCoinPrice.timestamp - 24 * 60 * 60
                val diff = latestCoinPrice.diff24h
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
