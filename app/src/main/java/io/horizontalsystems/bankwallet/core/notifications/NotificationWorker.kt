package io.horizontalsystems.bankwallet.core.notifications

import android.content.Context
import android.util.Log
import androidx.work.*
import io.horizontalsystems.bankwallet.core.App
import java.util.concurrent.TimeUnit

class NotificationWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            App.priceAlertManager.fetchNotifications()
            Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "doWork failed:", e)
            Result.success()//mark as success, otherwise it will cancel all work
        }
    }

    companion object {

        fun startPeriodicWorker(context: Context) {
            val workRequest = createPeriodicWorkRequest()

            WorkManager
                    .getInstance(context)
                    .enqueueUniquePeriodicWork("NotificationFetchWork", ExistingPeriodicWorkPolicy.KEEP, workRequest)
        }

        private fun createPeriodicWorkRequest() =
                PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
                        .setBackoffCriteria(BackoffPolicy.LINEAR, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                        .build()

    }

}
