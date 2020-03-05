package io.horizontalsystems.bankwallet.modules.settings.security

import android.app.Activity
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App

object SecuritySettingsModule {

    interface ISecuritySettingsView {
        fun setBackupAlertVisible(visible: Boolean)
        fun togglePinSet(pinSet: Boolean)
        fun setEditPinVisible(visible: Boolean)
        fun setBiometricSettingsVisible(visible: Boolean)
        fun toggleBiometricEnabled(enabled: Boolean)
        fun toggleTorEnabled(enabled: Boolean)
        fun showRestartAlert(checked: Boolean)
    }

    interface ISecuritySettingsViewDelegate {
        fun viewDidLoad()
        fun didTapManageKeys()
        fun didTapEditPin()
        fun didSwitchPinSet(enable: Boolean)
        fun didSwitchBiometricEnabled(enable: Boolean)
        fun didSwitchTorEnabled(checked: Boolean)
        fun didSetPin()
        fun didCancelSetPin()
        fun didUnlockPinToDisablePin()
        fun didCancelUnlockPinToDisablePin()
        fun onClear()
        fun didTapBlockchainSettings()
        fun setTorEnabled(checked: Boolean)
    }

    interface ISecuritySettingsInteractor {
        val allBackedUp: Boolean
        val biometricAuthSupported: Boolean
        val isPinSet: Boolean
        var isBiometricEnabled: Boolean
        var isTorEnabled: Boolean

        fun disablePin()
        fun clear()
        fun stopTor()
    }

    interface ISecuritySettingsInteractorDelegate {
        fun didAllBackedUp(allBackedUp: Boolean)
        fun didStopTor()
    }

    interface ISecuritySettingsRouter {
        fun showManageKeys()
        fun showEditPin()
        fun showSetPin()
        fun showUnlockPin()
        fun showBlockchainSettings()
        fun restartApp()
    }

    fun start(activity: Activity) {
        activity.startActivity(Intent(activity, SecuritySettingsActivity::class.java))
    }

    fun init(view: SecuritySettingsViewModel, router: ISecuritySettingsRouter) {
        val interactor = SecuritySettingsInteractor(App.backupManager, App.systemInfoManager, App.pinComponent, App.netKitManager)
        val presenter = SecuritySettingsPresenter(router, interactor)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }
}
