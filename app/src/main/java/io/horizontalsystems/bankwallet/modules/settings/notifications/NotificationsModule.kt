package io.horizontalsystems.bankwallet.modules.settings.notifications

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object NotificationsModule {

    fun start(context: Context) {
        val intent = Intent(context, NotificationsActivity::class.java)
        context.startActivity(intent)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val viewModel = NotificationsViewModel(App.priceAlertManager, App.walletManager, App.coinManager, App.notificationManager, App.localStorage)
            return viewModel as T
        }
    }
}

