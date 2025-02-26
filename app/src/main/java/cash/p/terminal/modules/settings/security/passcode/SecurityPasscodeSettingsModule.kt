package cash.p.terminal.modules.settings.security.passcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App

object SecurityPasscodeSettingsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SecuritySettingsViewModel(
                systemInfoManager = App.systemInfoManager,
                pinComponent = App.pinComponent,
                balanceHiddenManager = App.balanceHiddenManager,
                localStorage = App.localStorage
            ) as T
        }
    }

}
