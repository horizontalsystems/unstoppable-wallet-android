package io.horizontalsystems.bankwallet.modules.settings.security

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ISystemInfoManager
import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.bankwallet.entities.BiometryType

class SecuritySettingsInteractor(
        private val adapterManager: IAdapterManager,
        wordsManager: IWordsManager,
        private val localStorage: ILocalStorage,
        private val systemInfoManager: ISystemInfoManager): SecuritySettingsModule.ISecuritySettingsInteractor {

    var delegate: SecuritySettingsModule.ISecuritySettingsInteractorDelegate? = null

    init {
        val disposable = wordsManager.backedUpSubject.subscribe {
            onUpdateBackedUp(it)
        }
    }

    private fun onUpdateBackedUp(backedUp: Boolean) {
        if (backedUp) {
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
        adapterManager.clear()
        localStorage.clearAll()
        delegate?.didUnlinkWallet()
    }
}
