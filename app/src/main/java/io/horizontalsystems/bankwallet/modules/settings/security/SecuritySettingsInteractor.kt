package io.horizontalsystems.bankwallet.modules.settings.security

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.BiometryType

class SecuritySettingsInteractor(
        private val walletManager: IWalletManager,
        private val wordsManager: IWordsManager,
        private val localStorage: ILocalStorage,
        private val transactionRepository: ITransactionRecordStorage,
        private val exchangeRateRepository: IRateStorage,
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
        wordsManager.logout()
        delegate?.didUnlinkWallet()
    }
}
