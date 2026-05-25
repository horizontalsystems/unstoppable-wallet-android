package io.horizontalsystems.bankwallet.modules.settings.security.securesend

import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.address.AddressCheckType
import javax.inject.Inject

@HiltViewModel
class SecureSendConfigViewModel @Inject constructor(
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

data class SecureSendConfigUiState(
    val phishingEnabled: Boolean,
    val blacklistEnabled: Boolean,
    val sanctionsEnabled: Boolean,
)
