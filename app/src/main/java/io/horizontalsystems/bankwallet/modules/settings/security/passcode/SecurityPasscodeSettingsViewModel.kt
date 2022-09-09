package io.horizontalsystems.bankwallet.modules.settings.security.passcode

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SecurityPasscodeSettingsViewModel(
    private val service: SecurityPasscodeSettingsService
) : ViewModel() {

    var pinEnabled by mutableStateOf(service.isPinSet)
        private set

    var biometricSettingsVisible by mutableStateOf(service.isPinSet && service.isBiometricAuthSupported)
        private set

    var biometricEnabled by mutableStateOf(service.isBiometricAuthEnabled)
        private set


    fun setBiometricAuth(enabled: Boolean) {
        service.isBiometricAuthEnabled = enabled
        biometricEnabled = enabled
    }

    fun disablePin() {
        service.disablePin()
        syncPinSet(false)
    }

    fun didSetPin() {
        syncPinSet(true)
    }

    private fun syncPinSet(pinSet: Boolean) {
        pinEnabled = pinSet
        biometricSettingsVisible = pinSet && service.isBiometricAuthSupported
    }

}
