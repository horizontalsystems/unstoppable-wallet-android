package io.horizontalsystems.bankwallet.modules.settings.security

class SecuritySettingsPresenter(
        private val router: SecuritySettingsModule.ISecuritySettingsRouter,
        private val interactor: SecuritySettingsModule.ISecuritySettingsInteractor
) : SecuritySettingsModule.ISecuritySettingsViewDelegate {

    var view: SecuritySettingsModule.ISecuritySettingsView? = null

    override fun viewDidLoad() {
        syncPinSet(interactor.isPinSet)
        view?.toggleBiometricEnabled(interactor.isBiometricEnabled)
    }

    override fun didSwitchBiometricEnabled(enable: Boolean) {
        interactor.isBiometricEnabled = enable
    }

    override fun didSetPin() {
        syncPinSet(true)
        view?.toggleBiometricEnabled(interactor.isBiometricEnabled)
    }

    override fun didCancelSetPin() {
        view?.togglePinSet(false)
    }

    override fun didUnlockPinToDisablePin() {
        interactor.disablePin()
        syncPinSet(false)
    }

    override fun didCancelUnlockPinToDisablePin() {
        view?.togglePinSet(true)
    }

    private fun syncPinSet(pinSet: Boolean) {
        view?.togglePinSet(pinSet)
        view?.setEditPinVisible(pinSet)
        view?.setBiometricSettingsVisible(pinSet && interactor.biometricAuthSupported)
    }

    override fun didSwitchPinSet(enable: Boolean) {
        if (enable) {
            router.showSetPin()
        } else {
            router.showUnlockPin()
        }
    }

    override fun didTapEditPin() {
        router.showEditPin()
    }

    override fun didTapPrivacy() {
        router.openPrivacySettings()
    }

}
