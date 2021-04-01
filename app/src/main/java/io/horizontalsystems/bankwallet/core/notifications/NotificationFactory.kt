package io.horizontalsystems.bankwallet.core.notifications

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.AlertNotification

object NotificationFactory {

    fun getMessageFromJson(jsonObject: JsonObject?): AlertNotification? {
        jsonObject ?: return null

        try {
            val messageObject = jsonObject["message"].asJsonObject
            val title = messageObject["title-loc-key"].asString
            val topic = messageObject["loc-key"].asString
            val type = getNotificationType(topic) ?: return null
            val args = messageObject["loc-args"].asJsonArray

            val timestamp = jsonObject["timestamp"].asNumber.toLong()

            val coinCode = args[0].asString
            val notificationId = getNotificationId(topic, coinCode)
            val body = getBodyText(type, coinCode, topic, args)

            return AlertNotification(notificationId, title, body, timestamp)
        } catch (e: Exception) {
            Log.e("NotificationFactory", "Message parsing error", e)
            return null
        }
    }

    private fun getBodyText(type: NotificationType, coinCode: String, topic: String, args: JsonArray): String {
        return when (type) {
            NotificationType.Trend -> {
                val isDown = topic.contains("down")
                val stringRes = if (isDown) R.string.Notification_TrendDown else R.string.Notification_TrendUp
                Translator.getString(stringRes, coinCode)
            }
            NotificationType.Change -> {
                val changeValue = args[1].asFloat
                val isUp = changeValue > 0
                val stringRes = if (isUp) R.string.Notification_PriceUp else R.string.Notification_PriceDown
                Translator.getString(stringRes, coinCode, changeValue)
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

    enum class NotificationType {
        Change, Trend
    }

}
