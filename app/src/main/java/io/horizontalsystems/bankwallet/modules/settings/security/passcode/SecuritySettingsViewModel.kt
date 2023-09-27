package io.horizontalsystems.bankwallet.modules.settings.security.passcode

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class SecuritySettingsViewModel(
    private val systemInfoManager: ISystemInfoManager,
    private val pinComponent: IPinComponent,
    private val balanceHiddenManager: BalanceHiddenManager
) : ViewModel() {
    val biometricSettingsVisible = systemInfoManager.biometricAuthSupported

    private var pinEnabled = pinComponent.isPinSet
    private var biometricsEnabled = pinComponent.isBiometricAuthEnabled
    private var duressPinEnabled = pinComponent.isDuressPinSet()
    private var balanceAutoHideEnabled = balanceHiddenManager.balanceAutoHidden

    var uiState by mutableStateOf(
        SecuritySettingsUiState(
            pinEnabled = pinEnabled,
            biometricsEnabled = biometricsEnabled,
            duressPinEnabled = duressPinEnabled,
            balanceAutoHideEnabled = balanceAutoHideEnabled,
        )
    )
        private set

    init {
        viewModelScope.launch {
            pinComponent.pinSetFlowable.asFlow().collect {
                pinEnabled = pinComponent.isPinSet
                duressPinEnabled = pinComponent.isDuressPinSet()
                emitState()
            }
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = SecuritySettingsUiState(
                pinEnabled = pinEnabled,
                biometricsEnabled = biometricsEnabled,
                duressPinEnabled = duressPinEnabled,
                balanceAutoHideEnabled = balanceAutoHideEnabled
            )
        }
    }

    fun setBiometricAuth(enabled: Boolean) {
        pinComponent.isBiometricAuthEnabled = enabled
        biometricsEnabled = enabled
        emitState()
    }

    fun disablePin() {
        pinComponent.disablePin()
        pinComponent.isBiometricAuthEnabled = false
        biometricsEnabled = false
        emitState()
    }

    fun disableDuressPin() {
        pinComponent.disableDuressPin()
        emitState()
    }

    fun onSetBalanceAutoHidden(enabled: Boolean) {
        balanceAutoHideEnabled = enabled
        emitState()
        balanceHiddenManager.setBalanceAutoHidden(enabled)
    }
}

data class SecuritySettingsUiState(
    val pinEnabled: Boolean,
    val biometricsEnabled: Boolean,
    val duressPinEnabled: Boolean,
    val balanceAutoHideEnabled: Boolean
)