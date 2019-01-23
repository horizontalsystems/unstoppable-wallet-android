package io.horizontalsystems.bankwallet.modules.settings.security

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ISystemInfoManager
import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.bankwallet.core.managers.AuthManager
import io.horizontalsystems.bankwallet.entities.BiometryType

class SecuritySettingsInteractor(
        private val authManager: AuthManager,
        private val wordsManager: IWordsManager,
        private val localStorage: ILocalStorage,
        private val systemInfoManager: ISystemInfoManager): SecuritySettingsModule.ISecuritySettingsInteractor {

    var delegate: SecuritySettingsModule.ISecuritySettingsInteractorDelegate? = null

    init {
        wordsManager.backedUpSignal.subscribe {
            onUpdateBackedUp()
        }
    }

    private fun onUpdateBackedUp() {
        if (wordsManager.isBackedUp) {
            delegate?.didBackup()
        }
    }

    override var biometryType: BiometryType = BiometryType.NONE
        get() = systemInfoManager.biometryType

    override var isBackedUp: Boolean = wordsManager.isBackedUp

    override fun getBiometricUnlockOn(): Boolean {
        return localStorage.isBiometricOn
    }

    override fun setBiometricUnlockOn(biometricUnlockOn: Boolean) {
        localStorage.isBiometricOn = biometricUnlockOn
    }

    override fun unlinkWallet() {
        authManager.logout()
        delegate?.didUnlinkWallet()
    }
}
