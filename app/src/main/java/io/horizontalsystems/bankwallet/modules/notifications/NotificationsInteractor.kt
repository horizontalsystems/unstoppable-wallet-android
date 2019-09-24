package io.horizontalsystems.bankwallet.modules.notifications

import io.horizontalsystems.bankwallet.core.managers.BackgroundManager
import io.horizontalsystems.bankwallet.core.managers.NotificationManager
import io.horizontalsystems.bankwallet.core.managers.PriceAlertManager
import io.horizontalsystems.bankwallet.entities.PriceAlert

class NotificationsInteractor(
        private val priceAlertManager: PriceAlertManager,
        backgroundManager: BackgroundManager,
        private val notificationManager: NotificationManager
) : NotificationsModule.IInteractor, BackgroundManager.Listener {

    init {
        backgroundManager.registerListener(this)
    }

    lateinit var delegate: NotificationsModule.IInteractorDelegate

    override val priceAlertsEnabled: Boolean
        get() = notificationManager.isEnabled

    override val priceAlerts: List<PriceAlert>
        get() = priceAlertManager.getPriceAlerts()

    override fun savePriceAlert(priceAlert: PriceAlert) {
        priceAlertManager.savePriceAlert(priceAlert)
    }

    override fun willEnterForeground() {
        delegate.didEnterForeground()
    }
}
