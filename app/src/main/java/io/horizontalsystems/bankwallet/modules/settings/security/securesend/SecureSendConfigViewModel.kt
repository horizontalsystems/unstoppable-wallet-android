package io.horizontalsystems.bankwallet.modules.settings.security.securesend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState

class SecureSendConfigViewModel(
    private val localStorage: ILocalStorage
) : ViewModelUiState<SecureSendConfigUiState>() {

    override fun createState() = SecureSendConfigUiState(
        phishingEnabled = localStorage.phishingDetectionEnabled,
        blacklistEnabled = localStorage.blacklistDetectionEnabled,
        sanctionsEnabled = localStorage.sanctionsDetectionEnabled,
    )

    fun setPhishingEnabled(enabled: Boolean) {
        localStorage.phishingDetectionEnabled = enabled
        emitState()
    }

    fun setBlacklistEnabled(enabled: Boolean) {
        localStorage.blacklistDetectionEnabled = enabled
        emitState()
    }

    fun setSanctionsEnabled(enabled: Boolean) {
        localStorage.sanctionsDetectionEnabled = enabled
        emitState()
    }
}

object SecureSendConfigModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SecureSendConfigViewModel(App.localStorage) as T
        }
    }
}

data class SecureSendConfigUiState(
    val phishingEnabled: Boolean,
    val blacklistEnabled: Boolean,
    val sanctionsEnabled: Boolean,
)
