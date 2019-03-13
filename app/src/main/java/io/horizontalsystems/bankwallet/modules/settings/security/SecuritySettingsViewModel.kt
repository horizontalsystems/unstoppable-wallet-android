package io.horizontalsystems.bankwallet.modules.settings.security

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.BiometryType

class SecuritySettingsViewModel : ViewModel(), SecuritySettingsModule.ISecuritySettingsView, SecuritySettingsModule.ISecuritySettingsRouter {

    lateinit var delegate: SecuritySettingsModule.ISecuritySettingsViewDelegate

    val biometryTypeLiveDate = MutableLiveData<BiometryType>()

    val backedUpLiveData = MutableLiveData<Boolean>()
    val biometricUnlockOnLiveDate = MutableLiveData<Boolean>()
    val openEditPinLiveEvent = SingleLiveEvent<Unit>()
    val openBackupWalletLiveEvent = SingleLiveEvent<Unit>()
    val openRestoreWalletLiveEvent = SingleLiveEvent<Unit>()
    val reloadAppLiveEvent = SingleLiveEvent<Unit>()
    val showPinUnlockLiveEvent = SingleLiveEvent<Unit>()

    fun init() {
        SecuritySettingsModule.init(this, this)
        delegate.viewDidLoad()
    }

    override fun setBiometricUnlockOn(biometricUnlockOn: Boolean) {
        biometricUnlockOnLiveDate.value = biometricUnlockOn
    }

    override fun setBiometryType(biometryType: BiometryType) {
        biometryTypeLiveDate.value = biometryType
    }

    override fun setBackedUp(backedUp: Boolean) {
        backedUpLiveData.value = backedUp
    }

    override fun showEditPin() {
        openEditPinLiveEvent.call()
    }

    override fun showBackupWallet() {
        openBackupWalletLiveEvent.call()
    }

    override fun showRestoreWallet() {
        openRestoreWalletLiveEvent.call()
    }

    override fun reloadApp() {
        reloadAppLiveEvent.call()
    }

    override fun showPinUnlock() {
        showPinUnlockLiveEvent.call()
    }

    override fun onCleared() {
        delegate.onClear()
    }

}
