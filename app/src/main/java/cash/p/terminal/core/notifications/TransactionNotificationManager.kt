package cash.p.terminal.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cash.p.terminal.R
import cash.p.terminal.modules.main.MainActivity
import timber.log.Timber

class TransactionNotificationManager(
    private val context: Context,
) {

    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.push_notification_channel_transactions),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.push_notification_channel_transactions_desc)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun createServiceNotificationChannel() {
        val channel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            context.getString(R.string.push_notification_channel_service),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.push_notification_channel_service_desc)
            setShowBadge(false)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun buildServiceNotification() = NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(R.string.push_notification_service_title))
        .setContentText(context.getString(R.string.push_notification_service_text))
        .setOngoing(true)
        .setSilent(true)
        .build()

    fun showTransactionNotification(
        recordUid: String,
        title: String,
        text: String,
    ) {
        if (!hasNotificationPermission() || !isTransactionChannelEnabled()) {
            Timber.d("Notifications not available, skipping")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_RECORD_UID, recordUid)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            recordUid.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(recordUid, TRANSACTION_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Timber.w(e, "Notification permission revoked between check and notify")
        }
    }

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun checkAllPermissions(): Boolean {
        return hasNotificationPermission()
                && isTransactionChannelEnabled()
                && isServiceChannelEnabled()
    }

    fun isTransactionChannelEnabled() = isChannelEnabled(CHANNEL_ID)

    fun isServiceChannelEnabled() = isChannelEnabled(SERVICE_CHANNEL_ID)

    private fun isChannelEnabled(channelId: String): Boolean {
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(channelId) ?: return false
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    companion object {
        const val CHANNEL_ID = "transaction_notifications"
        const val SERVICE_CHANNEL_ID = "transaction_monitoring_service"
        const val SERVICE_NOTIFICATION_ID = 1001
        const val EXTRA_RECORD_UID = "open_record_uid"
        private const val TRANSACTION_NOTIFICATION_ID = 0
    }
}
