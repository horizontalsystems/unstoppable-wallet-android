package io.horizontalsystems.bankwallet.modules.settings.security.passcode

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager

class SecurityPasscodeSettingsViewModel(
    private val systemInfoManager: ISystemInfoManager,
    private val pinComponent: IPinComponent
) : ViewModel() {

    var pinEnabled by mutableStateOf(pinComponent.isPinSet)
        private set

    var biometricSettingsVisible by mutableStateOf(pinComponent.isPinSet && systemInfoManager.biometricAuthSupported)
        private set

    var biometricEnabled by mutableStateOf(pinComponent.isBiometricAuthEnabled)
        private set

    fun setBiometricAuth(enabled: Boolean) {
        pinComponent.isBiometricAuthEnabled = enabled
        biometricEnabled = enabled
    }

    fun disablePin() {
        pinComponent.clear()
        biometricEnabled = false
        syncPinSet(false)
    }

    fun didSetPin() {
        syncPinSet(true)
    }

    private fun syncPinSet(pinSet: Boolean) {
        pinEnabled = pinSet
        biometricSettingsVisible = pinSet && systemInfoManager.biometricAuthSupported
    }

}
