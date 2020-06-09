package io.horizontalsystems.bankwallet.modules.notifications

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PriceAlert

object NotificationsModule {
    interface IView {
        fun setItems(items: List<PriceAlertViewItem>)
        fun showWarning()
        fun hideWarning()
        fun showStateSelector(itemPosition: Int, priceAlert: PriceAlert)
        fun setNotificationSwitch(enabled: Boolean)
    }

    interface IRouter {
        fun openNotificationSettings()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun didSelectState(itemPosition: Int, state: PriceAlert.State)
        fun didClickOpenSettings()
        fun didClickDeactivateAll()
        fun didTapItem(itemPosition: Int)
        fun didSwitchAlertNotification(enabled: Boolean)
    }

    interface IInteractor {
        val priceAlertsEnabled: Boolean
        val priceAlerts: List<PriceAlert>
        var notificationIsOn: Boolean

        fun savePriceAlerts(priceAlerts: List<PriceAlert>)
        fun startBackgroundRateFetchWorker()
        fun stopBackgroundRateFetchWorker()
    }

    interface IInteractorDelegate {
        fun didEnterForeground()
    }

    fun start(context: Context) {
        val intent = Intent(context, NotificationsActivity::class.java)
        context.startActivity(intent)
    }

    data class PriceAlertViewItem(val title: String, val coin: Coin) {
        lateinit var state: PriceAlert.State

        constructor(title: String, coin: Coin, state: PriceAlert.State) : this(title, coin) {
            this.state = state
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = NotificationsView()
            val router = NotificationsRouter()
            val interactor = NotificationsInteractor(App.priceAlertManager, App.backgroundManager, App.localStorage, App.notificationManager, App.backgroundRateAlertScheduler)
            val presenter = NotificationsPresenter(view, router, interactor, PriceAlertViewItemFactory())

            interactor.delegate = presenter

            return presenter as T
        }
    }
}

