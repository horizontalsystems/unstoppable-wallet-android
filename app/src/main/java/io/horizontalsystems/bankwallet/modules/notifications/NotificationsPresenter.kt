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
//        priceAlerts = interactor.priceAlerts.sortedBy { it.coin.title }

//        view.setItems(priceAlertViewItemFactory.createItems(priceAlerts))
        view.setNotificationSwitch(interactor.notificationIsOn)

        checkPriceAlertsEnabled()
    }

    override fun didTapItem(itemPosition: Int) {
        view.showStateSelector(itemPosition, priceAlerts[itemPosition])
    }

//    override fun didSelectState(itemPosition: Int, state: PriceAlert.State) {
//        val priceAlert = priceAlerts[itemPosition]
//
//        if (priceAlert.state != state) {
//            priceAlert.state = state
//
//            interactor.savePriceAlerts(listOf(priceAlert))
//
//            view.setItems(priceAlertViewItemFactory.createItems(priceAlerts))
//        }
//    }

    override fun didClickOpenSettings() {
        router.openNotificationSettings()
    }

    override fun didClickDeactivateAll() {
//        priceAlerts.forEach { it.state = PriceAlert.State.OFF }

//        interactor.savePriceAlerts(priceAlerts)

//        view.setItems(priceAlertViewItemFactory.createItems(priceAlerts))
    }

    override fun didEnterForeground() {
        checkPriceAlertsEnabled()
    }

    override fun didSwitchAlertNotification(enabled: Boolean) {
        if (enabled) {
            interactor.startBackgroundRateFetchWorker()
        } else {
            interactor.stopBackgroundRateFetchWorker()
        }
        interactor.notificationIsOn = enabled
        view.setNotificationSwitch(interactor.notificationIsOn)
    }

    private fun checkPriceAlertsEnabled() {
        if (interactor.priceAlertsEnabled) {
            view.hideWarning()
        } else {
            view.showWarning()
        }
    }
}
