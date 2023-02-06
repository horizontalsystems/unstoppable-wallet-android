package io.horizontalsystems.marketkit.chart

import io.horizontalsystems.marketkit.NoChartData
import io.horizontalsystems.marketkit.managers.CoinManager
import io.horizontalsystems.marketkit.models.*
import io.horizontalsystems.marketkit.providers.HsProvider
import io.horizontalsystems.marketkit.storage.ChartPointStorage
import io.reactivex.Single
import java.util.*

class ChartManager(
    private val coinManager: CoinManager,
    private val storage: ChartPointStorage,
    private val provider: HsProvider,
    private val indicatorPoints: Int
) {

    var listener: Listener? = null

    interface Listener {
        fun onUpdate(chartInfo: ChartInfo, key: ChartInfoKey)
        fun noChartInfo(key: ChartInfoKey)
    }

    private fun chartInfo(points: List<ChartPoint>, periodType: HsPeriodType): ChartInfo? {
        val lastPoint = points.lastOrNull() ?: return null

        val lastPointTimestamp = lastPoint.timestamp
        val intervalRange = periodType.range
        val startTimestamp = intervalRange?.let {
            lastPointTimestamp - it
        } ?: points.first().timestamp

        val currentTimestamp = Date().time / 1000
        val lastPointGap = currentTimestamp - lastPointTimestamp

        // if points not in visible window (too early) just return null
        if (intervalRange != null && lastPointGap > intervalRange) {
            return null
        }

        return ChartInfo(
            points,
            startTimestamp,
            currentTimestamp,
            isExpired = lastPointGap > periodType.expiration
        )
    }

    private fun storedChartPoints(key: ChartInfoKey): List<ChartPoint> {
        return storage.getList(key.coin.uid, key.currencyCode, key.periodType).map { point ->
            ChartPoint(
                point.value,
                point.timestamp,
                point.volume?.let { mapOf(ChartPointType.Volume to it) } ?: emptyMap()
            )
        }
    }

    fun update(points: List<ChartPoint>, key: ChartInfoKey) {
        val records = points.map { point ->
            ChartPointEntity(
                key.coin.uid,
                key.currencyCode,
                key.periodType,
                point.value,
                point.extra[ChartPointType.Volume],
                point.timestamp,
            )
        }

        storage.delete(key)
        storage.save(records)

        val chartInfo = chartInfo(points, key.periodType)

        if (chartInfo == null) {
            listener?.noChartInfo(key)
        } else {
            listener?.onUpdate(chartInfo, key)
        }
    }

    fun handleNoChartPoints(key: ChartInfoKey) {
        listener?.noChartInfo(key)
    }

    fun getLastSyncTimestamp(key: ChartInfoKey): Long? {
        return storedChartPoints(key).lastOrNull()?.timestamp
    }

    fun getChartInfo(coinUid: String, currencyCode: String, periodType: HsPeriodType): ChartInfo? {
        val fullCoin = coinManager.fullCoins(listOf(coinUid)).firstOrNull() ?: return null
        val key = ChartInfoKey(fullCoin.coin, currencyCode, periodType)
        return chartInfo(storedChartPoints(key), periodType)
    }

    fun chartInfoSingle(
        coinUid: String,
        currencyCode: String,
        periodType: HsPeriodType
    ): Single<ChartInfo> {
        val fullCoin = coinManager.fullCoins(listOf(coinUid)).firstOrNull()
            ?: return Single.error(NoChartData())

        return provider.coinPriceChartSingle(
            fullCoin.coin.uid,
            currencyCode,
            periodType,
            indicatorPoints
        )
            .flatMap { response ->
                val points = response.map { it.chartPoint }

                chartInfo(points, periodType)?.let {
                    Single.just(it)
                } ?: Single.error(NoChartData())
            }
    }

    fun chartStartTimeSingle(coinUid: String): Single<Long> {
        return provider.coinPriceChartStartTime(coinUid)
    }
}
