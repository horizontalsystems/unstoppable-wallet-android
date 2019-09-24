package io.horizontalsystems.bankwallet.core.managers

import android.os.Build
import androidx.core.app.NotificationManagerCompat
import io.horizontalsystems.bankwallet.core.INotificationManager

class NotificationManager(private val androidNotificationManager: NotificationManagerCompat): INotificationManager {

    val isEnabled: Boolean
        get() = when {
            !androidNotificationManager.areNotificationsEnabled() -> false
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                val notificationChannel = androidNotificationManager.getNotificationChannel(channelId)

                notificationChannel?.importance != NotificationManagerCompat.IMPORTANCE_NONE
            }
            else -> true
        }



    companion object {
        private const val channelId = "priceAlert"
    }
}