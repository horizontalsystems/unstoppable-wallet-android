package io.horizontalsystems.bankwallet.modules.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object NotificationsModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val viewModel = NotificationsViewModel(App.priceAlertManager, App.walletManager, App.notificationManager, App.localStorage)
            return viewModel as T
        }
    }
}
