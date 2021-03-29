package io.horizontalsystems.bankwallet.core.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.AlertNotification
import java.lang.Exception


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        App.priceAlertManager.enablePriceAlerts()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            try {
                val title = remoteMessage.data["title-loc-key"] ?: return
                val topic = remoteMessage.data["loc-key"] ?: return
                val type = getNotificationType(topic) ?: return

                val stringListType = object : TypeToken<List<String>>() {}.type
                val args = Gson().fromJson<List<String>>(remoteMessage.data["loc-args"], stringListType)

                val coinCode = args[0]
                val notificationId = getNotificationId(topic, coinCode)
                val body = getBodyText(type, coinCode, topic, args)

                App.notificationManager.show(AlertNotification(notificationId, title, body))
            } catch (e: Exception) {
                Log.e(TAG, "Message parsing error", e)
            }
        }
    }

    private fun getBodyText(type: NotificationType, coinCode: String, topic: String, args: List<String>): String {
        return when (type) {
            NotificationType.Trend -> {
                val isDown = topic.contains("down")
                val stringRes = if (isDown) R.string.Notification_TrendDown else R.string.Notification_TrendUp
                Translator.string(stringRes, coinCode)
            }
            NotificationType.Change -> {
                val changeValue = args[1]
                val isUp = changeValue.toFloat() > 0
                val stringRes = if (isUp) R.string.Notification_PriceUp else R.string.Notification_PriceDown
                Translator.string(stringRes, coinCode, changeValue)
            }
        }
    }

    private fun getNotificationType(topic: String): NotificationType? {
        if (topic.contains("trend")) {
            return NotificationType.Trend
        } else if (topic.contains("change")) {
            return NotificationType.Change
        }
        return null
    }

    private fun getNotificationId(topic: String, coinCode: String): Int {
        val cleanTopic = topic
                .replace("_down", "")
                .replace("_up", "")
                .replace("_shortterm", "")
                .replace("_longterm", "")
        return "$cleanTopic$coinCode".hashCode()
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

    enum class NotificationType {
        Change, Trend
    }
}
