package io.horizontalsystems.marketkit.chart.scheduler

import io.reactivex.Single

interface IChartSchedulerProvider {

    val id: String
    val lastSyncTimestamp: Long?
    val expirationInterval: Long
    val retryInterval: Long
    val syncSingle: Single<Unit>

    fun notifyExpired()

}
