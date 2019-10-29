package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import androidx.work.*
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IBackgroundRateAlertScheduler
import io.reactivex.Single
import java.util.concurrent.TimeUnit

class BackgroundRateAlertScheduler(private val context: Context) : IBackgroundRateAlertScheduler {

    private val backgroundRateFetchWorkName = "BackgroundRateFetchWork"

    override fun startPeriodicWorker() {
        val rateFetcher = createPeriodicWorkRequest()
        WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(backgroundRateFetchWorkName, ExistingPeriodicWorkPolicy.REPLACE, rateFetcher)
    }

    override fun stopPeriodicWorker() {
        WorkManager
                .getInstance(context)
                .cancelUniqueWork(backgroundRateFetchWorkName)
    }


    private fun createPeriodicWorkRequest() =
            PeriodicWorkRequestBuilder<RateFetchWorker>(1, TimeUnit.HOURS)
                    .setConstraints(createConstraints())
                    .setBackoffCriteria(BackoffPolicy.LINEAR, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                    .build()

    private fun createConstraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
}

class RateFetchWorker(appContext: Context, workerParams: WorkerParameters) : RxWorker(appContext, workerParams) {
    override fun createWork(): Single<Result> {
        if (App.backgroundManager.inForeground) {
            return Single.just(Result.failure())
        }
        return App.backgroundPriceAlertManager.fetchRates()
                .map { Result.success() }
                .onErrorReturn { Result.failure() }
    }
}
