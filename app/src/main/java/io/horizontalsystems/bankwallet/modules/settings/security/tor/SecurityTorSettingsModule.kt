package io.horizontalsystems.bankwallet.modules.settings.security.tor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App

object SecurityTorSettingsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SecurityTorSettingsViewModel(App.torKitManager, App.pinComponent) as T
        }
    }

}

enum class TorStatus(val value: Int) {
    Connected(R.string.TorPage_Connected),
    Closed(R.string.TorPage_ConnectionClosed),
    Failed(R.string.TorPage_Failed),
    Connecting(R.string.TorPage_Connecting);

    val icon: Int?
        get() = when (this) {
            Connected -> R.drawable.ic_tor_connection_success_24
            Closed -> R.drawable.ic_tor_connection_24
            Failed -> R.drawable.ic_tor_connection_error_24
            Connecting -> null
        }

    val showConnectionSpinner: Boolean
        get() = when (this) {
            Connecting -> true
            else -> false
        }
}