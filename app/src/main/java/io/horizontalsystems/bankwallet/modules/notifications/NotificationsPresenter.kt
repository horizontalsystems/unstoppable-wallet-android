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

        val items = priceAlertViewItemFactory.createItems(priceAlerts)
        view.setItems(items)

        checkPriceAlertsEnabled()
    }

    override fun didSelectState(itemPosition: Int, state: PriceAlert.State) {
        val priceAlert = priceAlerts[itemPosition]
        priceAlert.state = state

        interactor.savePriceAlert(priceAlert)
    }

    override fun didClickOpenSettings() {
        router.openNotificationSettings()
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
