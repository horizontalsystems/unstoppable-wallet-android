package io.horizontalsystems.bankwallet.modules.settings.security.securesend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.address.AddressCheckType

class SecureSendConfigViewModel(
    private val localStorage: ILocalStorage
) : ViewModelUiState<SecureSendConfigUiState>() {

    override fun createState() = SecureSendConfigUiState(
        phishingEnabled = AddressCheckType.Phishing.name in localStorage.enabledPaidActions,
        blacklistEnabled = AddressCheckType.Blacklist.name in localStorage.enabledPaidActions,
        sanctionsEnabled = AddressCheckType.Sanction.name in localStorage.enabledPaidActions,
    )

    fun setPhishingEnabled(enabled: Boolean) = setDetectionEnabled(AddressCheckType.Phishing, enabled)
    fun setBlacklistEnabled(enabled: Boolean) = setDetectionEnabled(AddressCheckType.Blacklist, enabled)
    fun setSanctionsEnabled(enabled: Boolean) = setDetectionEnabled(AddressCheckType.Sanction, enabled)

    private fun setDetectionEnabled(type: AddressCheckType, enabled: Boolean) {
        val current = localStorage.enabledPaidActions.toMutableSet()
        if (enabled) current.add(type.name) else current.remove(type.name)
        localStorage.enabledPaidActions = current
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
