package io.horizontalsystems.bankwallet.modules.notifications

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.PriceAlert

class NotificationsPresenter(
        val view: NotificationsModule.IView,
        val router: NotificationsModule.IRouter,
        private val interactor: NotificationsModule.IInteractor,
        private val priceAlertViewItemFactory: PriceAlertViewItemFactory
) : ViewModel(), NotificationsModule.IViewDelegate, NotificationsModule.IInteractorDelegate  {

    private var priceAlerts = listOf<PriceAlert>()

    override fun viewDidLoad() {
        priceAlerts = interactor.priceAlerts

        view.setItems(priceAlertViewItemFactory.createItems(priceAlerts))

        checkPriceAlertsEnabled()
    }

    override fun didSelectState(itemPosition: Int, state: PriceAlert.State) {
        val priceAlert = priceAlerts[itemPosition]

        if (priceAlert.state != state) {
            priceAlert.state = state

            interactor.savePriceAlerts(listOf(priceAlert))
        }
    }

    override fun didClickOpenSettings() {
        router.openNotificationSettings()
    }

    override fun didClickDeactivateAll() {
        priceAlerts.forEach { it.state = PriceAlert.State.OFF }

        interactor.savePriceAlerts(priceAlerts)

        view.setItems(priceAlertViewItemFactory.createItems(priceAlerts))
    }

    override fun didEnterForeground() {
        checkPriceAlertsEnabled()
    }

    private fun checkPriceAlertsEnabled() {
        if (interactor.priceAlertsEnabled) {
            view.hideWarning()
        } else {
            view.showWarning()
        }
    }
}
