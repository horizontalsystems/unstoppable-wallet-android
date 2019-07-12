package io.horizontalsystems.bankwallet.modules.settings.security

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.BiometryType

object SecuritySettingsModule {

    interface ISecuritySettingsView {
        fun setBiometricUnlockOn(biometricUnlockOn: Boolean)
        fun setBiometryType(biometryType: BiometryType)
        fun setBackedUp(backedUp: Boolean)
    }

    interface ISecuritySettingsViewDelegate {
        fun viewDidLoad()
        fun didSwitchBiometricUnlock(biometricUnlockOn: Boolean)
        fun didTapManageKeys()
        fun didTapEditPin()
        fun onClear()
    }

    interface ISecuritySettingsInteractor {
        val nonBackedUpCount: Int
        val biometryType: BiometryType

        fun getBiometricUnlockOn(): Boolean
        fun setBiometricUnlockOn(biometricUnlockOn: Boolean)
        fun clear()
    }

    interface ISecuritySettingsInteractorDelegate {
        fun didBackup(count: Int)
    }

    interface ISecuritySettingsRouter {
        fun showManageKeys()
        fun showEditPin()
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, SecuritySettingsActivity::class.java))
    }

    fun init(view: SecuritySettingsViewModel, router: ISecuritySettingsRouter) {
        val interactor = SecuritySettingsInteractor(App.backupManager, App.localStorage, App.systemInfoManager)
        val presenter = SecuritySettingsPresenter(router, interactor)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }
}
