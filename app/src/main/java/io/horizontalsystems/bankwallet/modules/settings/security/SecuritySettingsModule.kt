package io.horizontalsystems.bankwallet.modules.settings.security

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.NetManager

object SecuritySettingsModule {

    interface ISecuritySettingsView {
        fun setBackupAlertVisible(visible: Boolean)
        fun togglePinSet(pinSet: Boolean)
        fun setEditPinVisible(visible: Boolean)
        fun setBiometricSettingsVisible(visible: Boolean)
        fun toggleBiometricEnabled(enabled: Boolean)
        fun toggleTorEnabled(enabled: Boolean)
    }

    interface ISecuritySettingsViewDelegate {
        fun viewDidLoad()
        fun didTapManageKeys()
        fun didTapEditPin()
        fun didSwitchPinSet(enable: Boolean)
        fun didSwitchBiometricEnabled(enable: Boolean)
        fun didSwitchTorEnabled(enable: Boolean)
        fun didSetPin()
        fun didCancelSetPin()
        fun didUnlockPinToDisablePin()
        fun didCancelUnlockPinToDisablePin()
        fun onClear()
        fun didTapBlockchainSettings()
    }

    interface ISecuritySettingsInteractor {
        val allBackedUp: Boolean
        val biometricAuthSupported: Boolean
        val isPinSet: Boolean
        var isBiometricEnabled: Boolean
        var isTorEnabled: Boolean

        fun disablePin()
        fun clear()
    }

    interface ISecuritySettingsInteractorDelegate {
        fun didAllBackedUp(allBackedUp: Boolean)
    }

    interface ISecuritySettingsRouter {
        fun showManageKeys()
        fun showEditPin()
        fun showSetPin()
        fun showUnlockPin()
        fun showBlockchainSettings()
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, SecuritySettingsActivity::class.java))
    }

    fun init(view: SecuritySettingsViewModel, router: ISecuritySettingsRouter) {
        val interactor = SecuritySettingsInteractor(App.backupManager, App.localStorage, App.systemInfoManager, App.pinManager, NetManager())
        val presenter = SecuritySettingsPresenter(router, interactor)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }
}
