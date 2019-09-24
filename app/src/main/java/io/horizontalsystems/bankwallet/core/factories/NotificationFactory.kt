package io.horizontalsystems.bankwallet.core.factories

import android.content.Context
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IEmojiHelper
import io.horizontalsystems.bankwallet.core.INotificationFactory
import io.horizontalsystems.bankwallet.entities.AlertNotification
import io.horizontalsystems.bankwallet.entities.Coin
import kotlin.math.abs

class NotificationFactory(
        private val emojiHelper: IEmojiHelper,
        private val context: Context
) : INotificationFactory {

    private val separatedAlertCount = 2

    override fun notifications(priceAlertItems: List<PriceAlertItem>): List<AlertNotification> {
        if (priceAlertItems.size <= separatedAlertCount) {
            val alertNotifications = mutableListOf<AlertNotification>()
            priceAlertItems.forEach { item ->
                val title = item.coin.title + " " + emojiHelper.title(item.state)
                val directionString = context.getString(if (item.state > 0) R.string.Notification_Up else R.string.Notification_Down)
                val body = directionString + " ${abs(item.state)}% " + emojiHelper.body(item.state)

                alertNotifications.add(AlertNotification(title, "", body))
            }
            return alertNotifications
        } else {
            val bodyParts = mutableListOf<String>()
            val sortedItems = priceAlertItems.sortedByDescending { it.state }

            sortedItems.forEach { item ->
                val directionString = context.getString(if (item.state > 0) R.string.Notification_Up else R.string.Notification_Down)
                val body = item.coin.code + " " + directionString + " ${abs(item.state)}%"
                bodyParts.add(body)
            }
            val title = context.getString(R.string.Notification_Notifications, emojiHelper.multiAlerts)
            val body = bodyParts.joinToString(", ")
            return listOf(AlertNotification(title, "", body))
        }
    }
}

class PriceAlertItem(val coin: Coin, val state: Int)
