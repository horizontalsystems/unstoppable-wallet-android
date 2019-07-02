package io.horizontalsystems.bankwallet.modules.settings.security

class SecuritySettingsPresenter(
        private val router: SecuritySettingsModule.ISecuritySettingsRouter,
        private val interactor: SecuritySettingsModule.ISecuritySettingsInteractor)
    : SecuritySettingsModule.ISecuritySettingsViewDelegate, SecuritySettingsModule.ISecuritySettingsInteractorDelegate {

    var view: SecuritySettingsModule.ISecuritySettingsView? = null

    override fun viewDidLoad() {
        view?.setBiometricUnlockOn(interactor.getBiometricUnlockOn())
        view?.setBiometryType(interactor.biometryType)
        view?.setBackedUp(interactor.isBackedUp)
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

    override fun didTapBackupWallet() {
        interactor.didTapOnBackupWallet()
    }

    override fun didTapRestoreWallet() {
        router.showRestoreWallet()
    }

    override fun confirmedUnlinkWallet() {
        interactor.unlinkWallet()
    }

    override fun onClear() {
        interactor.clear()
    }

    // ISecuritySettingsInteractorDelegate

    override fun didBackup() {
        view?.setBackedUp(true)
    }

    override fun didUnlinkWallet() {
        view?.reloadApp()
    }

    override fun openBackupWallet() {
        router.showBackupWallet()
    }

    override fun accessIsRestricted() {
        router.showPinUnlock()
    }

}
