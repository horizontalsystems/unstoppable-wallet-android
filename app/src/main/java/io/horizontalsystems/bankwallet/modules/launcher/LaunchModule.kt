package io.horizontalsystems.bankwallet.modules.launcher

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.utils.RootUtil
import io.horizontalsystems.core.security.KeyStoreValidationResult

object LaunchModule {

    interface IView

    interface IViewDelegate {
        fun viewDidLoad()
        fun didUnlock()
        fun didCancelUnlock()
    }

    interface IInteractor {
        val isPinNotSet: Boolean
        val isAccountsEmpty: Boolean
        val isSystemLockOff: Boolean
        val isDeviceRooted: Boolean
        val skipRootCheck: Boolean
        val mainShowedOnce: Boolean

        fun validateKeyStore(): KeyStoreValidationResult
    }

    interface IInteractorDelegate

    interface IRouter {
        fun openWelcomeModule()
        fun openMainModule()
        fun openUnlockModule()
        fun closeApplication()
        fun openNoSystemLockModule()
        fun openKeyInvalidatedModule()
        fun openUserAuthenticationModule()
        fun openDeviceIsRootedWarning()
    }

    fun init(view: LaunchViewModel, router: IRouter) {
        val interactor = LaunchInteractor(App.accountManager, App.pinComponent, App.systemInfoManager, App.keyStoreManager, RootUtil, App.buildConfigProvider, App.localStorage)
        val presenter = LaunchPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(context: Context) {
        val intent = Intent(context, LauncherActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

}
