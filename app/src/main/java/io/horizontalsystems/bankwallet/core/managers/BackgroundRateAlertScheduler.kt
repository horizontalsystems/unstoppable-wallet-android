package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import androidx.work.*
import io.horizontalsystems.bankwallet.core.App
import io.reactivex.Single
import java.util.concurrent.TimeUnit

class BackgroundRateAlertScheduler(appContext: Context, workerParams: WorkerParameters) : RxWorker(appContext, workerParams) {

    override fun createWork(): Single<Result> {
        if (App.backgroundManager.inForeground){
            return Single.just(Result.failure())
        }
        return App.backgroundPriceAlertManager.fetchRates()
                .map { Result.success() }
                .onErrorReturn { Result.failure() }
    }

    companion object {

        fun startPeriodicWorker(context: Context) {
            val workRequest = createPeriodicWorkRequest()

            WorkManager
                    .getInstance(context)
                    .enqueueUniquePeriodicWork("BackgroundRateFetchWork", ExistingPeriodicWorkPolicy.REPLACE, workRequest)
        }

        private fun createPeriodicWorkRequest() =
                PeriodicWorkRequestBuilder<BackgroundRateAlertScheduler>(1, TimeUnit.HOURS)
                        .setConstraints(createConstraints())
                        .setBackoffCriteria(BackoffPolicy.LINEAR, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                        .build()

        private fun createConstraints() = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
    }
}
