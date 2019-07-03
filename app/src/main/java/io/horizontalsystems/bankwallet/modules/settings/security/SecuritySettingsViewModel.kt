package io.horizontalsystems.bankwallet.modules.settings.security

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.BiometryType

class SecuritySettingsViewModel : ViewModel(), SecuritySettingsModule.ISecuritySettingsView, SecuritySettingsModule.ISecuritySettingsRouter {

    lateinit var delegate: SecuritySettingsModule.ISecuritySettingsViewDelegate

    val biometryTypeLiveDate = MutableLiveData<BiometryType>()

    val backedUpLiveData = MutableLiveData<Boolean>()
    val biometricUnlockOnLiveDate = MutableLiveData<Boolean>()
    val openManageKeysLiveEvent = SingleLiveEvent<Unit>()
    val openEditPinLiveEvent = SingleLiveEvent<Unit>()
    val openBackupWalletLiveEvent = SingleLiveEvent<Unit>()
    val openRestoreWalletLiveEvent = SingleLiveEvent<Unit>()
    val reloadAppLiveEvent = SingleLiveEvent<Unit>()
    val showPinUnlockLiveEvent = SingleLiveEvent<Unit>()

    fun init() {
        SecuritySettingsModule.init(this, this)
        delegate.viewDidLoad()
    }

    //  ViewModel

    override fun onCleared() {
        delegate.onClear()
    }

    //  ISecuritySettingsView

    override fun setBiometricUnlockOn(biometricUnlockOn: Boolean) {
        biometricUnlockOnLiveDate.value = biometricUnlockOn
    }

    override fun setBiometryType(biometryType: BiometryType) {
        biometryTypeLiveDate.value = biometryType
    }

    override fun setBackedUp(backedUp: Boolean) {
        backedUpLiveData.postValue(backedUp)
    }

    override fun reloadApp() {
        reloadAppLiveEvent.call()
    }

    //  ISecuritySettingsRouter

    override fun showManageKeys() {
        openManageKeysLiveEvent.call()
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

    override fun showPinUnlock() {
        showPinUnlockLiveEvent.call()
    }

}
