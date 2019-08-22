package io.horizontalsystems.bankwallet.modules.settings.security

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IPinManager
import io.horizontalsystems.bankwallet.core.ISystemInfoManager
import io.reactivex.disposables.CompositeDisposable

class SecuritySettingsInteractor(
        private val localStorage: ILocalStorage,
        private val systemInfoManager: ISystemInfoManager,
        private val pinManager: IPinManager)
    : SecuritySettingsModule.ISecuritySettingsInteractor {

    private var disposables: CompositeDisposable = CompositeDisposable()

    override val hasFingerprintSensor: Boolean
        get() = systemInfoManager.hasFingerprintSensor

    override val hasEnrolledFingerprints: Boolean
        get() = systemInfoManager.hasEnrolledFingerprints

    override var isFingerPrintEnabled: Boolean
        get() = localStorage.isFingerprintEnabled
        set(value) {
            localStorage.isFingerprintEnabled = value
        }

    override val isPinEnabled: Boolean
        get() = pinManager.isPinSet

    override fun disablePin() {
        pinManager.clear()
    }

    override fun clear() {
        disposables.clear()
    }

}
