package io.horizontalsystems.bankwallet.modules.settings.security

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.BiometryType

object SecuritySettingsModule {

    interface ISecuritySettingsView {
        fun setTitle(title: Int)
        fun setBiometricUnlockOn(biometricUnlockOn: Boolean)
        fun setBiometryType(biometryType: BiometryType)
        fun setBackedUp(backedUp: Boolean)
        fun reloadApp()
    }

    interface ISecuritySettingsViewDelegate {
        fun viewDidLoad()
        fun didSwitchBiometricUnlock(biometricUnlockOn: Boolean)
        fun didTapEditPin()
        fun didTapBackupWallet()
        fun didTapRestoreWallet()
        fun confirmedUnlinkWallet()
    }

    interface ISecuritySettingsInteractor {
        var isBackedUp: Boolean
        val biometryType: BiometryType
        fun getBiometricUnlockOn(): Boolean
        fun setBiometricUnlockOn(biometricUnlockOn: Boolean)
        fun unlinkWallet()
        fun didTapOnBackupWallet()
    }

    interface ISecuritySettingsInteractorDelegate {
        fun didBackup()
        fun didUnlinkWallet()
        fun openBackupWallet()
        fun accessIsRestricted()
    }

    interface ISecuritySettingsRouter{
        fun showEditPin()
        fun showBackupWallet()
        fun showRestoreWallet()
        fun showPinUnlock()
    }

    fun start(context: Context) {
        val intent = Intent(context, SecuritySettingsActivity::class.java)
        context.startActivity(intent)
    }

    fun init(view: SecuritySettingsViewModel, router: ISecuritySettingsRouter) {
        val interactor = SecuritySettingsInteractor(App.authManager, App.wordsManager, App.localStorage, App.systemInfoManager, App.lockManager)
        val presenter = SecuritySettingsPresenter(router, interactor)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }
}
