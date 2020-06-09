package io.horizontalsystems.bankwallet.modules.notifications

import io.horizontalsystems.bankwallet.entities.PriceAlert

class PriceAlertViewItemFactory {

    fun createItems(priceAlerts: List<PriceAlert>): List<NotificationsModule.PriceAlertViewItem> {
        return priceAlerts.map { alert ->
            NotificationsModule.PriceAlertViewItem(alert.coin.title, alert.coin, alert.state)
        }
    }

}
