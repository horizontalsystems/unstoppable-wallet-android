package io.horizontalsystems.bankwallet.widgets

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.*
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.modules.launcher.LauncherActivity
import java.net.UnknownHostException
import java.time.Duration

class MarketWidgetWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    companion object {
        private const val updatePeriodMillis: Long = 15 * 60 * 1000 // 15 minutes
        private const val inputDataKeyWidgetId = "widgetIdKey"
        private const val notificationChannelName = "MARKET_WIDGET_CHANNEL_NAME"
        private const val notificationChannelId = "MARKET_WIDGET_CHANNEL_ID"
        private val pendingIntentFlagMutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

        private val logger = AppLogger("widget-worker")

        private fun uniqueWorkName(widgetId: Int) = "${MarketWidgetWorker::class.java.simpleName}_${widgetId}"

        fun enqueue(context: Context, widgetId: Int) {
            val manager = WorkManager.getInstance(context)
            val requestBuilder = PeriodicWorkRequestBuilder<MarketWidgetWorker>(Duration.ofMillis(updatePeriodMillis))

            val inputData = Data.Builder().putInt(inputDataKeyWidgetId, widgetId).build()
            requestBuilder.setInputData(inputData)

            val uniqueWorkName = uniqueWorkName(widgetId)

            manager.enqueueUniquePeriodicWork(
                uniqueWorkName,
                ExistingPeriodicWorkPolicy.REPLACE,
                requestBuilder.build()
            )
        }

        fun cancel(context: Context, widgetId: Int) {
            val uniqueWorkName = uniqueWorkName(widgetId)

            WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)
        }
    }

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(MarketWidget::class.java)
        val currentTimestampMillis = System.currentTimeMillis()
        val widgetId = inputData.getInt(inputDataKeyWidgetId, 0)
        val marketRepository = App.marketWidgetRepository

        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                setForeground(getForegroundInfo())
            }

            for (glanceId in glanceIds) {
                var state = getAppWidgetState(context, MarketWidgetStateDefinition, glanceId)
                if (state.widgetId != widgetId) continue

                val imagePathCache = buildMap {
                    state.items.forEach { item ->
                        item.imageLocalPath?.let { set(item.imageRemoteUrl, it) }
                    }
                }
                var marketItems = marketRepository.getMarketItems(state.type)
                marketItems = marketItems.map { it.copy(imageLocalPath = imagePathCache[it.imageRemoteUrl]) }

                state = state.copy(items = marketItems, loading = false, error = null)
                setWidgetState(glanceId, state)

                marketItems = marketItems.map { item ->
                    item.copy(imageLocalPath = item.imageLocalPath ?: getImage(item.imageRemoteUrl))
                }

                state = state.copy(items = marketItems, updateTimestampMillis = currentTimestampMillis)
                setWidgetState(glanceId, state)

                break
            }

            Result.success()
        } catch (exception: Exception) {
            logger.info("doWork error, widgetId: $widgetId, ${exception.javaClass.simpleName}: ${exception.message}")

            glanceIds.forEach { glanceId ->
                var state = getAppWidgetState(context, MarketWidgetStateDefinition, glanceId)

                if (state.widgetId == widgetId) {
                    val errorText = if (exception is UnknownHostException)
                        context.getString(R.string.Hud_Text_NoInternet)
                    else {
                        context.getString(R.string.SyncError) + "\n\n\n" + "[ ${state.error} ]"
                    }

                    state = state.copy(loading = false, error = errorText)
                    setWidgetState(glanceId, state)
                }
            }
            if (runAttemptCount < 10) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private suspend fun getImage(url: String): String? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .build()

        with(context.imageLoader) {
            val result = execute(request)
            if (result is ErrorResult) {
                return null
            }
        }

        val localPath = context.imageLoader.diskCache?.get(url)?.use { snapshot ->
            snapshot.data.toFile().path
        }

        return localPath
    }

    private suspend fun setWidgetState(glanceId: GlanceId, state: MarketWidgetState) {
        updateAppWidgetState(context, MarketWidgetStateDefinition, glanceId) {
            state
        }
        MarketWidget().update(context, glanceId)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val widgetId = inputData.getInt(inputDataKeyWidgetId, 0)
        val channelId = "$notificationChannelId-$widgetId"
        val channelName = "$notificationChannelName-$widgetId"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, LauncherActivity::class.java),
                    pendingIntentFlagMutable
                )
            )
            .setSmallIcon(R.drawable.ic_refresh)
            .setOngoing(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setLocalOnly(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentText("Updating widget")
            .build()

        return ForegroundInfo(1234, notification)
    }

}
