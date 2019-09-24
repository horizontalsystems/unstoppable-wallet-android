package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.IRateStatsManager
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.core.managers.ServiceExchangeApi.HostType
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.RateData
import io.horizontalsystems.bankwallet.entities.RateStatData
import io.horizontalsystems.bankwallet.lib.chartview.ChartHelper
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.*

class RateStatsManager(private val networkManager: INetworkManager, private val rateStorage: IRateStorage)
    : IRateStatsManager {

    private val cacheUpdateTimeInterval: Long = 30 * 60 * 60 // 30 minutes in seconds
    private val disposables = CompositeDisposable()
    private val cache = mutableMapOf<StatsKey, Pair<Long?, RateStatData>>()
    private val statsSubject = PublishSubject.create<StatsResponse>()

    override val statsFlowable: Flowable<StatsResponse>
        get() = statsSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun syncStats(coinCode: String, currencyCode: String) {
        val statsKey = StatsKey(coinCode, currencyCode)
        val currentTime = Date().time / 1000 // timestamp in seconds
        val cached = cache[statsKey]

        val rateStats = if (cached != null && cached.first ?: 0 > currentTime - cacheUpdateTimeInterval) {
            Single.just(cached.second)
        } else {
            networkManager.getRateStats(HostType.MAIN, coinCode, currencyCode)
                    .onErrorResumeNext { networkManager.getRateStats(HostType.FALLBACK, coinCode, currencyCode) }
        }

        val rateLocal = rateStorage.latestRateObservable(coinCode, currencyCode).firstOrError()

        Single.zip(rateLocal, rateStats, BiFunction<Rate, RateStatData, Pair<Rate, RateStatData>> { a, b -> Pair(a, b) })
                .map { (rate, data) ->
                    val lastDailyTimestamp = data.stats[ChartType.DAILY.name]?.timestamp
                    cache[statsKey] = Pair(lastDailyTimestamp, data)

                    val stats = mutableMapOf<String, List<ChartPoint>>()
                    val diffs = mutableMapOf<String, BigDecimal>()

                    for (type in data.stats.keys) {
                        val statsData = data.stats[type] ?: continue
                        val chartType = ChartType.fromString(type) ?: continue
                        val chartData = convert(statsData, rate, chartType)

                        stats[type] = chartData
                        diffs[type] = growthDiff(chartData)
                    }

                    StatsData(coinCode, data.marketCap, stats, diffs)
                }
                .subscribeOn(Schedulers.io())
                .subscribe({
                    statsSubject.onNext(it)
                }, {
                    statsSubject.onNext(StatsError(coinCode))
                })
                .let { disposables.add(it) }
    }

    fun clear() {
        disposables.clear()
        cache.clear()
    }

    private fun convert(data: RateData, rate: Rate?, chartType: ChartType): List<ChartPoint> {
        val rates = data.rates.toMutableList()
        var timestamp = data.timestamp
        if (rate != null) {
            timestamp = rate.timestamp
            rates.add(rate.value.toFloat())
        }

        val points = when (chartType) {
            ChartType.MONTHLY18 -> rates.takeLast(ChartType.annualPoints) // for one year
            else -> rates
        }

        return ChartHelper.convert(points, data.scale, timestamp)
    }

    private fun growthDiff(points: List<ChartPoint>): BigDecimal {
        val pointStart = points.first { it.value != 0f }
        val pointEnd = points.last()

        return ((pointEnd.value - pointStart.value) / pointStart.value * 100).toBigDecimal()
    }
}

sealed class StatsResponse

data class StatsKey(val coinCode: String, val currencyCode: String)
data class StatsData(val coinCode: String, val marketCap: BigDecimal, val stats: Map<String, List<ChartPoint>>, val diff: Map<String, BigDecimal>) : StatsResponse()
data class StatsError(val coinCode: String) : StatsResponse()
