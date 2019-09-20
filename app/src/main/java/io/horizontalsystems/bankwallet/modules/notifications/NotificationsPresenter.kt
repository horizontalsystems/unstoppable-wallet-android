package io.horizontalsystems.bankwallet.modules.notifications

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.PriceAlert

class NotificationsPresenter(
        val view: NotificationsModule.IView,
        private val interactor: NotificationsModule.IInteractor,
        private val priceAlertViewItemFactory: PriceAlertViewItemFactory
) : ViewModel(), NotificationsModule.IViewDelegate {

    private var priceAlerts = listOf<PriceAlert>()

    override fun viewDidLoad() {
        priceAlerts = interactor.priceAlerts

        val items = priceAlertViewItemFactory.createItems(priceAlerts)
        view.setItems(items)
    }

    override fun didSelectState(itemPosition: Int, state: PriceAlert.State) {
        val priceAlert = priceAlerts[itemPosition]
        priceAlert.state = state

        interactor.savePriceAlert(priceAlert)
    }
}
