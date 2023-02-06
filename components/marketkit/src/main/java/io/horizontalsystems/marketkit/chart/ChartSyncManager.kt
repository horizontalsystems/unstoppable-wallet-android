package io.horizontalsystems.marketkit.chart

import io.horizontalsystems.marketkit.NoChartInfo
import io.horizontalsystems.marketkit.managers.CoinManager
import io.horizontalsystems.marketkit.models.ChartInfo
import io.horizontalsystems.marketkit.models.ChartInfoKey
import io.horizontalsystems.marketkit.chart.scheduler.ChartScheduler
import io.horizontalsystems.marketkit.models.HsPeriodType
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class ChartSyncManager(
    private val coinManager: CoinManager,
    private val factory: ChartSchedulerFactory
) : ChartManager.Listener {

    private val subjects = ConcurrentHashMap<ChartInfoKey, PublishSubject<ChartInfo>>()
    private val schedulers = ConcurrentHashMap<ChartInfoKey, ChartScheduler>()
    private val observers = ConcurrentHashMap<ChartInfoKey, AtomicInteger>()

    private val failedKeys = ConcurrentLinkedQueue<ChartInfoKey>()
    private val disposables = ConcurrentHashMap<ChartInfoKey, Disposable>()

    fun chartInfoObservable(coinUid: String, currencyCode: String, interval: HsPeriodType): Observable<ChartInfo> {

        val fullCoin = coinManager.fullCoins(listOf(coinUid)).firstOrNull() ?: return Observable.error(NoChartInfo())

        val key = ChartInfoKey(fullCoin.coin, currencyCode, interval)

        if (failedKeys.contains(key)) {
            return Observable.error(NoChartInfo())
        }

        return getSubject(key)
            .doOnSubscribe {
                getCounter(key).incrementAndGet()
                getScheduler(key).start()
            }
            .doOnDispose {
                getCounter(key).decrementAndGet()
                cleanup(key)
            }
            .doOnError {
                getCounter(key).decrementAndGet()
                cleanup(key)
            }
    }

    //  ChartManager Listener

    override fun onUpdate(chartInfo: ChartInfo, key: ChartInfoKey) {
        subjects[key]?.onNext(chartInfo)
    }

    override fun noChartInfo(key: ChartInfoKey) {
        failedKeys.add(key)
        if (subjects[key]?.hasObservers() == true) {
            subjects[key]?.onError(NoChartInfo())
        }
    }

    @Synchronized
    private fun getSubject(key: ChartInfoKey): Observable<ChartInfo> {
        var subject = subjects[key]
        if (subject == null) {
            subject = PublishSubject.create<ChartInfo>()
            subjects[key] = subject
        }

        return subject
    }

    @Synchronized
    private fun getScheduler(key: ChartInfoKey): ChartScheduler {
        var scheduler = schedulers[key]
        if (scheduler == null) {
            scheduler = factory.getScheduler(key)
            schedulers[key] = scheduler
        }

        return scheduler
    }

    private fun cleanup(key: ChartInfoKey) {
        val subject = subjects[key]
        if (subject == null || getCounter(key).get() > 0) {
            return
        }

        subject.onComplete()
        subjects.remove(key)

        schedulers[key]?.stop()
        schedulers.remove(key)

        disposables[key]?.dispose()
    }

    private fun getCounter(key: ChartInfoKey): AtomicInteger {
        var count = observers[key]
        if (count == null) {
            count = AtomicInteger(0)
            observers[key] = count
        }

        return count
    }
}
