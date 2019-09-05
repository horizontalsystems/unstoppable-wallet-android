package io.horizontalsystems.bankwallet.modules.settings.security

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App

object SecuritySettingsModule {

    interface ISecuritySettingsView {
        fun setBackedUp(backedUp: Boolean)
        fun setPinEnabled(enabled: Boolean)
        fun showFingerprintSettings(enabled: Boolean)
        fun hideFingerprintSettings()
        fun showNoEnrolledFingerprints()
    }

    interface ISecuritySettingsViewDelegate {
        fun viewDidLoad()
        fun didTapManageKeys()
        fun didTapEditPin()
        fun didTapEnablePin(enable: Boolean)
        fun didTapEnableFingerprint(enable: Boolean)
        fun didSetPin()
        fun didCancelSetPin()
        fun didUnlockPinToDisablePin()
        fun didCancelUnlockPinToDisablePin()
        fun onClear()
    }

    interface ISecuritySettingsInteractor {
        val allBackedUp: Boolean
        val hasFingerprintSensor: Boolean
        val hasEnrolledFingerprints: Boolean
        val isPinEnabled: Boolean
        var isFingerPrintEnabled: Boolean

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
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, SecuritySettingsActivity::class.java))
    }

    fun init(view: SecuritySettingsViewModel, router: ISecuritySettingsRouter) {
        val interactor = SecuritySettingsInteractor(App.backupManager, App.localStorage, App.systemInfoManager, App.pinManager)
        val presenter = SecuritySettingsPresenter(router, interactor)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }
}
