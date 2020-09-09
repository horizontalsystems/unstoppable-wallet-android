package io.horizontalsystems.bankwallet.modules.settings.notifications

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App

object NotificationsModule {

    fun start(activity: FragmentActivity) {
        activity.supportFragmentManager.commit {
            add(R.id.fragmentContainerView, NotificationsFragment())
            addToBackStack(null)
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val viewModel = NotificationsViewModel(App.priceAlertManager, App.walletManager, App.coinManager, App.notificationManager, App.localStorage)
            return viewModel as T
        }
    }
}
