package io.horizontalsystems.bankwallet.modules.settings.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper

object AboutModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AboutViewModel(App.appConfigProvider, TextHelper, App.rateAppManager, App.termsManager, App.systemInfoManager) as T
        }
    }
}
