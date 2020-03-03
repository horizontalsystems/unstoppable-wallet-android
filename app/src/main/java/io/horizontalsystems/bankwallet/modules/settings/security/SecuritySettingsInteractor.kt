package io.horizontalsystems.bankwallet.modules.settings.security

import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.bankwallet.core.INetManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class SecuritySettingsInteractor(
        private val backupManager: IBackupManager,
        private val systemInfoManager: ISystemInfoManager,
        private val pinComponent: IPinComponent,
        private val netManager: INetManager)
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
        get() = pinComponent.isFingerprintEnabled
        set(value) {
            pinComponent.isFingerprintEnabled = value
        }

    override var isTorEnabled: Boolean
        get() = netManager.isTorEnabled
        set(value) {
            pinComponent.updateLastExitDateBeforeRestart()
            if (value) {
                netManager.enableTor()
            } else {
                netManager.disableTor()
            }
        }

    override val isPinSet: Boolean
        get() = pinComponent.isPinSet

    override fun disablePin() {
        pinComponent.clear()
    }

    override fun clear() {
        disposables.clear()
    }

    override fun stopTor() {
        disposables.add(netManager.stop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    delegate?.didStopTor()
                }, {

                }))
    }
}
