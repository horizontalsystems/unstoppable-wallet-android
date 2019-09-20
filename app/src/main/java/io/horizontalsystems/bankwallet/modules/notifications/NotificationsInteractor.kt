package io.horizontalsystems.bankwallet.modules.notifications

import io.horizontalsystems.bankwallet.core.managers.PriceAlertManager
import io.horizontalsystems.bankwallet.entities.PriceAlert

class NotificationsInteractor(private val priceAlertManager: PriceAlertManager) : NotificationsModule.IInteractor {

    override val priceAlerts: List<PriceAlert>
        get() = priceAlertManager.getPriceAlerts()

    override fun savePriceAlert(priceAlert: PriceAlert) {
        priceAlertManager.savePriceAlert(priceAlert)
    }
}
