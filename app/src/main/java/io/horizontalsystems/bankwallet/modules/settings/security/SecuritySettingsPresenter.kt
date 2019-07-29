package io.horizontalsystems.bankwallet.modules.settings.security

class SecuritySettingsPresenter(private val router: SecuritySettingsModule.ISecuritySettingsRouter, private val interactor: SecuritySettingsModule.ISecuritySettingsInteractor)
    : SecuritySettingsModule.ISecuritySettingsViewDelegate, SecuritySettingsModule.ISecuritySettingsInteractorDelegate {

    var view: SecuritySettingsModule.ISecuritySettingsView? = null

    override fun viewDidLoad() {
        view?.setBiometricUnlockOn(interactor.getBiometricUnlockOn())
        view?.setBiometryType(interactor.biometryType)
        view?.setBackedUp(interactor.nonBackedUpCount == 0)
        view?.setPinEnabled(interactor.isPinSet)
    }

    override fun didSwitchBiometricUnlock(biometricUnlockOn: Boolean) {
        interactor.setBiometricUnlockOn(biometricUnlockOn)
    }

    override fun didTapManageKeys() {
        router.showManageKeys()
    }

    override fun didTapEditPin() {
        router.showEditPin()
    }

    override fun didTapEnablePin(enable: Boolean) {
        if (enable) {
            router.showSetPin()
        } else {
            router.showUnlockPin()
        }
    }

    override fun didSetPin() {
        view?.setPinEnabled(true)
    }

    override fun didCancelSetPin() {
        view?.setPinEnabled(false)
    }

    override fun didUnlockPinToDisablePin() {
        interactor.disablePin()
        view?.setPinEnabled(false)
    }

    override fun didCancelUnlockPinToDisablePin() {
        view?.setPinEnabled(true)
    }

    override fun onClear() {
        interactor.clear()
    }

    // ISecuritySettingsInteractorDelegate

    override fun didBackup(count: Int) {
        view?.setBackedUp(count == 0)
    }
}
