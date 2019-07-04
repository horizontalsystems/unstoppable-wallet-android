package io.horizontalsystems.bankwallet.modules.settings.security

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ISystemInfoManager
import io.horizontalsystems.bankwallet.entities.BiometryType
import io.reactivex.disposables.CompositeDisposable

class SecuritySettingsInteractor(
        private val accountManager: IAccountManager,
        private val localStorage: ILocalStorage,
        private val systemInfoManager: ISystemInfoManager)
    : SecuritySettingsModule.ISecuritySettingsInteractor {

    var delegate: SecuritySettingsModule.ISecuritySettingsInteractorDelegate? = null
    private var disposables: CompositeDisposable = CompositeDisposable()

    init {
        accountManager.nonBackedUpCountFlowable
                .subscribe { delegate?.didBackup(it) }
                .let { disposables.add(it) }
    }

    override val biometryType: BiometryType
        get() = systemInfoManager.biometryType

//    override val nonBackedUpCount: Int
//        get() = accountManager.nonBackedUpCount

    override fun getBiometricUnlockOn(): Boolean {
        return localStorage.isBiometricOn
    }

    override fun setBiometricUnlockOn(biometricUnlockOn: Boolean) {
        localStorage.isBiometricOn = biometricUnlockOn
    }

    override fun clear() {
        disposables.clear()
    }
}
