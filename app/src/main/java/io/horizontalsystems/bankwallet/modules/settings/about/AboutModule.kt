package io.horizontalsystems.bankwallet.modules.settings.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object AboutModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = AboutService(App.appConfigProvider, App.termsManager, App.systemInfoManager)
            return AboutViewModel(service) as T
        }
    }
}
