package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object MainSettingsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = MainSettingsService(
                App.backupManager,
                App.languageManager,
                App.systemInfoManager,
                App.currencyManager,
                App.termsManager,
                App.pinComponent,
                App.wc1SessionManager,
                App.wc2SessionManager,
                App.wc1Manager
            )
            val viewModel = MainSettingsViewModel(
                service,
                App.appConfigProvider.companyWebPageLink,
            )

            return viewModel as T
        }
    }

}
