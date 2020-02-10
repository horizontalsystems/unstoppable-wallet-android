package io.horizontalsystems.bankwallet.modules.settings.security

import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.NetManager
import io.horizontalsystems.core.IPinManager
import io.horizontalsystems.core.ISystemInfoManager
import io.reactivex.disposables.CompositeDisposable

class SecuritySettingsInteractor(
        private val backupManager: IBackupManager,
        private val localStorage: ILocalStorage,
        private val systemInfoManager: ISystemInfoManager,
        private val pinManager: IPinManager,
        private val netManager: NetManager)
    : SecuritySettingsModule.ISecuritySettingsInteractor {

    var delegate: SecuritySettingsModule.ISecuritySettingsInteractorDelegate? = null
    private var disposables: CompositeDisposable = CompositeDisposable()

    init {
        backupManager.allBackedUpFlowable
                .subscribe { delegate?.didAllBackedUp(it) }
                .let { disposables.add(it) }
    }

    override val biometricAuthSupported: Boolean
        get() = systemInfoManager.biometricAuthSupported

    override val allBackedUp: Boolean
        get() = backupManager.allBackedUp

    override var isBiometricEnabled: Boolean
        get() = pinManager.isFingerprintEnabled
        set(value) {
            pinManager.isFingerprintEnabled = value
        }

    override var isTorEnabled: Boolean
        get() = localStorage.torEnabled
        set(value) {
            if (value) {
                netManager.start()
            } else {
                netManager.stop()
            }
            localStorage.torEnabled = value
        }

    override val isPinSet: Boolean
        get() = pinManager.isPinSet

    override fun disablePin() {
        pinManager.clear()
    }

    override fun clear() {
        disposables.clear()
    }

}
