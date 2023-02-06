package io.horizontalsystems.marketkit

import io.horizontalsystems.marketkit.providers.ISchedulerProvider
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

class Scheduler(
    private val provider: ISchedulerProvider,
    private val bufferInterval: Int = 5
) {
    private val retryInterval = 30L
    private var timeDisposable: Disposable? = null
    private var syncDisposable: Disposable? = null

    private var isExpiredRatesNotified = false

    @Volatile
    private var stopped = false

    fun start(force: Boolean = false) {
        //  Force sync
        if (force) {
            onFire()
        } else {
            autoSchedule()
        }
    }

    @Synchronized
    fun stop() {
        stopped = true
        timeDisposable?.dispose()
        syncDisposable?.dispose()
    }

    private fun autoSchedule(minDelay: Long = 0) {
        var newDelay = 0L

        provider.lastSyncTimestamp?.let { lastSync ->
            val diff = Date().time / 1000 - lastSync
            newDelay = max(0, provider.expirationInterval - bufferInterval - diff)
        }

        val delay = max(newDelay, minDelay)
        schedule(delay)
    }

    @Synchronized
    private fun schedule(delay: Long) {
        if (stopped) return

        notifyRatesIfExpired()

        timeDisposable?.dispose()
        timeDisposable = Observable
            .timer(delay, TimeUnit.SECONDS)
            .subscribe({
                onFire()
            }, {
                it.printStackTrace()
            })
    }

    private fun onFire() {
        if (stopped) return

        syncDisposable?.dispose()
        syncDisposable = provider.syncSingle
            .subscribeOn(Schedulers.io())
            .subscribe({
                autoSchedule(retryInterval)
                isExpiredRatesNotified = false
            }, {
                schedule(retryInterval)
            })
    }

    private fun notifyRatesIfExpired() {
        if (isExpiredRatesNotified) return

        val timestamp = provider.lastSyncTimestamp
        if (timestamp == null || Date().time / 1000 - timestamp > provider.expirationInterval) {
            provider.notifyExpired()
            isExpiredRatesNotified = true
        }
    }
}
