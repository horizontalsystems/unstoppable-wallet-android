package io.horizontalsystems.bankwallet.modules.settings.security

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class SecuritySettingsViewModel : ViewModel(), SecuritySettingsModule.ISecuritySettingsView, SecuritySettingsModule.ISecuritySettingsRouter {

    lateinit var delegate: SecuritySettingsModule.ISecuritySettingsViewDelegate

    val openManageKeysLiveEvent = SingleLiveEvent<Unit>()
    val openEditPinLiveEvent = SingleLiveEvent<Unit>()
    val openSetPinLiveEvent = SingleLiveEvent<Unit>()
    val openUnlockPinLiveEvent = SingleLiveEvent<Unit>()
    val pinEnabledLiveEvent = SingleLiveEvent<Boolean>()
    val showFingerprintSettings = SingleLiveEvent<Boolean>()
    val hideFingerprintSettings = SingleLiveEvent<Unit>()
    val showNoEnrolledFingerprints = SingleLiveEvent<Unit>()

    fun init() {
        SecuritySettingsModule.init(this, this)
        delegate.viewDidLoad()
    }

    //  ViewModel

    override fun onCleared() {
        delegate.onClear()
    }

    //  ISecuritySettingsView

    override fun setPinEnabled(enabled: Boolean) {
        pinEnabledLiveEvent.postValue(enabled)
    }

    override fun showFingerprintSettings(enabled: Boolean) {
        showFingerprintSettings.postValue(enabled)
    }

    override fun hideFingerprintSettings() {
        hideFingerprintSettings.postValue(null)
    }

    override fun showNoEnrolledFingerprints() {
        showNoEnrolledFingerprints.postValue(null)
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

    override fun showUnlockPin() {
        openUnlockPinLiveEvent.call()
    }
}
