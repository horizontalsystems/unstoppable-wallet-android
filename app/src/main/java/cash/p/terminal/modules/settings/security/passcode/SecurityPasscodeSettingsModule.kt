package cash.p.terminal.modules.settings.security.passcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App

object SecurityPasscodeSettingsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = SecurityPasscodeSettingsService(App.systemInfoManager, App.pinComponent)
            return SecurityPasscodeSettingsViewModel(service) as T
        }
    }

}
