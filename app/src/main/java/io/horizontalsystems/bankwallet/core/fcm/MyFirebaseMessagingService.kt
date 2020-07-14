package io.horizontalsystems.bankwallet.core.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AlertNotification
import java.lang.Exception


class MyFirebaseMessagingService: FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        App.priceAlertManager.enablePriceAlerts()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            try {
                val title = remoteMessage.data["title-loc-key"] ?: return
                val topic = remoteMessage.data["loc-key"] ?: return
                val stringListType = object : TypeToken<List<String>>() {}.type
                val args = Gson().fromJson<List<String>>(remoteMessage.data["loc-args"], stringListType)

                val coinCode = args[0]
                val changeValue = args[1]
                val isUp = changeValue.toFloat() > 0

                val notificationId = getNotificationId(topic, coinCode)
                val notification = getAlertNotification(notificationId, title, coinCode, changeValue, isUp)
                App.notificationManager.show(notification)
            } catch (e: Exception){
                Log.e(TAG, "Message parsing error", e)
            }
        }
    }

    private fun getNotificationId(topic: String, coinCode: String): Int {
        val cleanTopic = topic.replace("_down", "").replace("_up", "")
        return "$cleanTopic$coinCode".hashCode()
    }

    private fun getAlertNotification(notificationId: Int, title: String, coinCode: String, value: String, isUp: Boolean) : AlertNotification {
        val stringRes = if (isUp) R.string.Notification_PriceUp else R.string.Notification_PriceDown
        val body = App.instance.getString(stringRes, coinCode, value)

        return AlertNotification(id = notificationId, title = title, body = body)
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
