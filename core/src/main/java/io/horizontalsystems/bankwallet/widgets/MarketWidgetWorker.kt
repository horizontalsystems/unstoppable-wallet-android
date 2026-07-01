package io.horizontalsystems.bankwallet.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
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

        fun enqueueWork(context: Context) {
            val manager = WorkManager.getInstance(context)
            val requestBuilder = PeriodicWorkRequestBuilder<MarketWidgetWorker>(Duration.ofMillis(updatePeriodMillis))

            manager.enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.UPDATE,
                requestBuilder.build()
            )
        }

        fun cancel(context: Context) {
            if (!hasEnabledWidgets(context)) {
                WorkManager.getInstance(context).cancelUniqueWork(workName)
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

        for (glanceId in glanceIds) {
            marketWidgetManager.refresh(glanceId)
        }
        return Result.success()
    }

}
