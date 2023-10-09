package io.horizontalsystems.bankwallet.modules.settings.security.passcode

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class SecuritySettingsViewModel(
    private val systemInfoManager: ISystemInfoManager,
    private val pinComponent: IPinComponent,
    private val balanceHiddenManager: BalanceHiddenManager,
    private val localStorage: ILocalStorage
) : ViewModel() {
    val biometricSettingsVisible = systemInfoManager.biometricAuthSupported

    private var pinEnabled = pinComponent.isPinSet
    private var duressPinEnabled = pinComponent.isDuressPinSet()
    private var balanceAutoHideEnabled = balanceHiddenManager.balanceAutoHidden

    var uiState by mutableStateOf(
        SecuritySettingsUiState(
            pinEnabled = pinEnabled,
            biometricsEnabled = pinComponent.isBiometricAuthEnabled,
            duressPinEnabled = duressPinEnabled,
            balanceAutoHideEnabled = balanceAutoHideEnabled,
            autoLockIntervalName = localStorage.autoLockInterval.title,
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
                biometricsEnabled = pinComponent.isBiometricAuthEnabled,
                duressPinEnabled = duressPinEnabled,
                balanceAutoHideEnabled = balanceAutoHideEnabled,
                autoLockIntervalName = localStorage.autoLockInterval.title,
            )
        }
    }

    fun enableBiometrics() {
        pinComponent.isBiometricAuthEnabled = true
        emitState()
    }

    fun disableBiometrics() {
        pinComponent.isBiometricAuthEnabled = false
        emitState()
    }

    fun disablePin() {
        pinComponent.disablePin()
        pinComponent.isBiometricAuthEnabled = false
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

    fun update() {
        emitState()
    }
}

data class SecuritySettingsUiState(
    val pinEnabled: Boolean,
    val biometricsEnabled: Boolean,
    val duressPinEnabled: Boolean,
    val balanceAutoHideEnabled: Boolean,
    val autoLockIntervalName: Int
)