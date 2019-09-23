package io.horizontalsystems.bankwallet.modules.notifications

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.PriceAlert

object NotificationsModule {
    interface IView {
        fun setItems(items: List<PriceAlertViewItem>)
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun didSelectState(itemPosition: Int, state: PriceAlert.State)
    }

    interface IInteractor {
        val priceAlerts: List<PriceAlert>

        fun savePriceAlert(priceAlert: PriceAlert)
    }

    fun start(context: Context) {
        val intent = Intent(context, NotificationsActivity::class.java)
        context.startActivity(intent)
    }

    data class PriceAlertViewItem(val title: String, val code: String) {
        lateinit var state: PriceAlert.State

        constructor(title: String, code: String, state: PriceAlert.State) : this(title, code) {
            this.state = state
        }
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = NotificationsView()
            val interactor = NotificationsInteractor(App.priceAlertManager)
            val presenter = NotificationsPresenter(view, interactor, PriceAlertViewItemFactory())

            return presenter as T
        }
    }
}

