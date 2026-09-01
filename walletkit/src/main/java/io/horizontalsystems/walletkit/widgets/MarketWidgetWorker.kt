package io.horizontalsystems.walletkit.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration

class MarketWidgetWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    companion object {
        private const val updatePeriodMillis: Long = 15 * 60 * 1000 // 15 minutes
        private const val workName = "widget_update_work"
        private const val syncWhenOnlineWorkName = "widget_sync_when_online"
        private val connectedConstraints = Constraints(requiredNetworkType = NetworkType.CONNECTED)

        fun enqueueWork(context: Context) {
            val manager = WorkManager.getInstance(context)
            val requestBuilder = PeriodicWorkRequestBuilder<MarketWidgetWorker>(Duration.ofMillis(updatePeriodMillis))
                // Without a network the fetch can only fail; let WorkManager wait for connectivity
                // instead of burning the run on retries.
                .setConstraints(connectedConstraints)

            manager.enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.UPDATE,
                requestBuilder.build()
            )
        }

        /**
         * One-shot refresh that runs as soon as the device is online. Enqueued after a failed
         * refresh so the widget recovers from Doze/offline without waiting for the next
         * periodic slot. KEEP: one pending sync is enough.
         */
        fun enqueueSyncWhenOnline(context: Context) {
            if (!hasEnabledWidgets(context)) return
            val request = OneTimeWorkRequestBuilder<MarketWidgetWorker>()
                .setConstraints(connectedConstraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                syncWhenOnlineWorkName,
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            if (!hasEnabledWidgets(context)) {
                val manager = WorkManager.getInstance(context)
                manager.cancelUniqueWork(workName)
                manager.cancelUniqueWork(syncWhenOnlineWorkName)
            }
        }

        fun hasEnabledWidgets(context: Context): Boolean {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = getWidgetIds(context, appWidgetManager)
            widgetIds.forEach { widgetId ->
                if (appWidgetManager.getAppWidgetInfo(widgetId) != null) {
                    return true
                }
            }
            return false
        }

        private fun getWidgetIds(context: Context, appWidgetManager: AppWidgetManager): IntArray {
            val widgetComponent = ComponentName(context, MarketWidgetReceiver::class.java)
            return appWidgetManager.getAppWidgetIds(widgetComponent)
        }
    }

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(MarketWidget::class.java)
        val marketWidgetManager = MarketWidgetManager()

        // Awaited: returning before the refresh finishes lets the process be killed mid-fetch,
        // which left widgets showing stale rates.
        var allUpdated = true
        for (glanceId in glanceIds) {
            if (!marketWidgetManager.refresh(glanceId)) {
                allUpdated = false
            }
        }
        return if (allUpdated) Result.success() else Result.retry()
    }

}
