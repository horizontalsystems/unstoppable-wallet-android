package io.horizontalsystems.bankwallet.modules.settings.security

import io.horizontalsystems.bankwallet.R

class SecuritySettingsPresenter(
        private val router: SecuritySettingsModule.ISecuritySettingsRouter,
        private val interactor: SecuritySettingsModule.ISecuritySettingsInteractor)
    : SecuritySettingsModule.ISecuritySettingsViewDelegate, SecuritySettingsModule.ISecuritySettingsInteractorDelegate {

    var view: SecuritySettingsModule.ISecuritySettingsView? = null

    override fun viewDidLoad() {
        view?.setTitle(R.string.Settings_SecurityCenter)
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
        interactor.didTapOnBackupWallet()
    }

    override fun accessIsRestricted() {
        router.showPinUnlock()
    }

    override fun openBackupWallet() {
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

    override fun onClear() {
        interactor.clear()
    }

}
