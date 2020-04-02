package io.horizontalsystems.bankwallet.modules.settings.security

import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager

class SecuritySettingsInteractor(
        private val systemInfoManager: ISystemInfoManager,
        private val pinComponent: IPinComponent
) : SecuritySettingsModule.ISecuritySettingsInteractor {

    override val biometricAuthSupported: Boolean
        get() = systemInfoManager.biometricAuthSupported

    override var isBiometricEnabled: Boolean
        get() = pinComponent.isFingerprintEnabled
        set(value) {
            pinComponent.isFingerprintEnabled = value
        }

    override val isPinSet: Boolean
        get() = pinComponent.isPinSet

    override fun disablePin() {
        pinComponent.clear()
    }

}
