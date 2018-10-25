package bitcoin.wallet.modules.settings.security

import bitcoin.wallet.core.IAdapterManager
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.ISystemInfoManager
import bitcoin.wallet.core.IWordsManager
import bitcoin.wallet.entities.BiometryType

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
