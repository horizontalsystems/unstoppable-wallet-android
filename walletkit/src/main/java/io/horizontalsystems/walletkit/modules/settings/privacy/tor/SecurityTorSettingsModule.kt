package io.horizontalsystems.walletkit.modules.settings.privacy.tor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App

object SecurityTorSettingsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SecurityTorSettingsViewModel(App.torKitManager, App.pinComponent) as T
        }
    }

}

enum class TorStatus {
    Connected,
    Closed,
    Failed,
    Connecting;
}