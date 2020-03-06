package io.horizontalsystems.bankwallet.core.managers

import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.INotificationManager
import io.horizontalsystems.bankwallet.entities.AlertNotification
import io.horizontalsystems.bankwallet.modules.launcher.LauncherActivity
import android.app.NotificationManager as SystemNotificationManager

class NotificationManager(private val androidNotificationManager: NotificationManagerCompat) : INotificationManager {

    private val maxNumberOfNotifications = 2

    override val isEnabled: Boolean
        get() = when {
            !androidNotificationManager.areNotificationsEnabled() -> false
            else -> {
                val notificationChannel = androidNotificationManager.getNotificationChannel(channelId)
                notificationChannel?.importance != NotificationManagerCompat.IMPORTANCE_NONE
            }
        }

    override fun show(notifications: List<AlertNotification>) {
        createNotificationChannel()
        clear()

        notifications.forEachIndexed { index, notification ->
            showNotification(index, notification)
        }
    }

    override fun clear() {
        for(i in 0 until maxNumberOfNotifications){
            androidNotificationManager.cancel(i)
        }
    }

    private fun showNotification(notificationId: Int, notification: AlertNotification) {
        val builder = NotificationCompat.Builder(App.instance, channelId)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentTitle(notification.title)
                .setContentText(notification.body)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(notification.body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(getPendingIntent())
                .setAutoCancel(true)

        androidNotificationManager.notify(notificationId, builder.build())
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(App.instance, LauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(App.instance, 0, intent, 0)
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = App.instance.getString(R.string.App_Name)
            val importance = SystemNotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance)
            // Register the channel with the system
            androidNotificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val channelId = "priceAlert"
    }
}
