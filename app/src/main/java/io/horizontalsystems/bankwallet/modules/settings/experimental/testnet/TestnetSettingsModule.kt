package io.horizontalsystems.bankwallet.modules.settings.experimental.testnet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object TestnetSettingsModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TestnetSettingsViewModel(TestnetSettingsService(App.evmTestnetManager)) as T
        }
    }
}
