package bitcoin.wallet.modules.settings.security

import bitcoin.wallet.R

class SecuritySettingsPresenter(
        private val router: SecuritySettingsModule.ISecuritySettingsRouter,
        private val interactor: SecuritySettingsModule.ISecuritySettingsInteractor)
    : SecuritySettingsModule.ISecuritySettingsViewDelegate, SecuritySettingsModule.ISecuritySettingsInteractorDelegate {

    var view: SecuritySettingsModule.ISecuritySettingsView? = null

    override fun viewDidLoad() {
        view?.setTitle(R.string.settings_security_center)
        view?.setBiometricUnlockOn(interactor.getBiometricUnlockOn())
        view?.setBiometryType(interactor.biometryType)
        view?.setBackedUp(interactor.isBackedUp)
    }

    override fun didSwitchBiometricUnlock(biometricUnlockOn: Boolean) {
        interactor.setBiometricUnlockOn(biometricUnlockOn)
    }

    override fun didTapEditPin() {
        router.showEditPin()
    }

    override fun didTapBackupWallet() {
        router.showBackupWallet()
    }

    override fun didTapRestoreWallet() {
        router.showRestoreWallet()
    }

    override fun confirmedUnlinkWallet() {
        interactor.unlinkWallet()
    }

    override fun didBackup() {
        view?.setBackedUp(true)
    }

    override fun didUnlinkWallet() {
        view?.reloadApp()
    }
}
