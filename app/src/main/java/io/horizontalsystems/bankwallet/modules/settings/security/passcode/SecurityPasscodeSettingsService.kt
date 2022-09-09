package io.horizontalsystems.bankwallet.modules.settings.security.passcode

import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager

class SecurityPasscodeSettingsService(
    private val systemInfoManager: ISystemInfoManager,
    private val pinComponent: IPinComponent
) {

    val isBiometricAuthSupported: Boolean
        get() = systemInfoManager.biometricAuthSupported

    var isBiometricAuthEnabled: Boolean
        get() = pinComponent.isBiometricAuthEnabled
        set(value) {
            pinComponent.isBiometricAuthEnabled = value
        }

    val isPinSet: Boolean
        get() = pinComponent.isPinSet

    fun disablePin() {
        pinComponent.clear()
    }

}
