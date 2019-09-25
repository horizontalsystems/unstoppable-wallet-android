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

    override fun notifications(alertItems: List<PriceAlertItem>): List<AlertNotification> {
        return when {
            alertItems.size <= separatedAlertCount -> alertItems.map { singleNotification(it) }
            else -> listOf(multipleNotification(alertItems))
        }
    }

    private fun singleNotification(item: PriceAlertItem) : AlertNotification {
        val title = item.coin.title + " " + emojiHelper.title(item.state)
        val directionString = context.getString(if (item.state > 0) R.string.Notification_Up else R.string.Notification_Down)
        val body = directionString + " ${abs(item.state)}% " + emojiHelper.body(item.state)

        return AlertNotification(title, body)
    }

    private fun multipleNotification(items: List<PriceAlertItem>) : AlertNotification {
        val title = context.getString(R.string.Notification_Notifications, emojiHelper.multiAlerts)
        val sortedItems = items.sortedByDescending { it.state }
        val body = sortedItems.map { bodyPart(it) }.joinToString()

        return AlertNotification(title, body)
    }

    private fun bodyPart(item: PriceAlertItem) : String {
        val directionString = context.getString(if (item.state > 0) R.string.Notification_Up else R.string.Notification_Down)
        return item.coin.code + " " + directionString + " ${abs(item.state)}%"
    }

}

class PriceAlertItem(val coin: Coin, val state: Int)
