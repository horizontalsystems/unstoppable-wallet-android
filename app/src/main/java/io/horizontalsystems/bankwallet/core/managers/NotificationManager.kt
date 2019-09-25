package io.horizontalsystems.bankwallet.core.managers

import android.os.Build
import androidx.core.app.NotificationManagerCompat
import io.horizontalsystems.bankwallet.core.INotificationManager
import io.horizontalsystems.bankwallet.entities.AlertNotification

class NotificationManager(private val androidNotificationManager: NotificationManagerCompat): INotificationManager {

    override val isEnabled: Boolean
        get() = when {
            !androidNotificationManager.areNotificationsEnabled() -> false
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                val notificationChannel = androidNotificationManager.getNotificationChannel(channelId)

                notificationChannel?.importance != NotificationManagerCompat.IMPORTANCE_NONE
            }
            else -> true
        }

    override fun show(notifications: List<AlertNotification>) {
        //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        private const val channelId = "priceAlert"
    }
}