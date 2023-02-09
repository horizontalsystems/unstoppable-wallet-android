package io.horizontalsystems.bankwallet.modules.settings.security.passcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object SecurityPasscodeSettingsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SecurityPasscodeSettingsViewModel(App.systemInfoManager, App.pinComponent) as T
        }
    }

}
