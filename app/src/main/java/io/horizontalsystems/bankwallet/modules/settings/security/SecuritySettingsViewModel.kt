package io.horizontalsystems.bankwallet.modules.settings.security

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.BiometryType

class SecuritySettingsViewModel : ViewModel(), SecuritySettingsModule.ISecuritySettingsView, SecuritySettingsModule.ISecuritySettingsRouter {

    lateinit var delegate: SecuritySettingsModule.ISecuritySettingsViewDelegate

    val biometryTypeLiveData = MutableLiveData<BiometryType>()
    val backedUpLiveData = MutableLiveData<Boolean>()
    val biometricUnlockOnLiveData = MutableLiveData<Boolean>()
    val openManageKeysLiveEvent = SingleLiveEvent<Unit>()
    val openEditPinLiveEvent = SingleLiveEvent<Unit>()
    val openSetPinLiveEvent = SingleLiveEvent<Unit>()
    val pinEnabledLiveEvent = MutableLiveData<Boolean>()

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
        biometricUnlockOnLiveData.value = biometricUnlockOn
    }

    override fun setBiometryType(biometryType: BiometryType) {
        biometryTypeLiveData.value = biometryType
    }

    override fun setBackedUp(backedUp: Boolean) {
        backedUpLiveData.postValue(backedUp)
    }

    override fun setPinEnabled(enabled: Boolean) {
        pinEnabledLiveEvent.postValue(enabled)
    }

    //  ISecuritySettingsRouter

    override fun showManageKeys() {
        openManageKeysLiveEvent.call()
    }

    override fun showEditPin() {
        openEditPinLiveEvent.call()
    }

    override fun showSetPin() {
        openSetPinLiveEvent.call()
    }
}
