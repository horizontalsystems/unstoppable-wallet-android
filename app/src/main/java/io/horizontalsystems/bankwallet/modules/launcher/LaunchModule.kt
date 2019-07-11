package io.horizontalsystems.bankwallet.modules.launcher

import io.horizontalsystems.bankwallet.core.App

object LaunchModule {

    interface IView {
        fun showNoDeviceLockWarning()
    }

    interface IViewDelegate {
        fun viewDidLoad()
    }

    interface IInteractor {
        val isPinNotSet: Boolean
        val isAccountsEmpty: Boolean
        val isDeviceLockDisabled: Boolean
    }

    interface IInteractorDelegate

    interface IRouter {
        fun openWelcomeModule()
        fun openMainModule()
        fun openUnlockModule()
    }

    fun init(view: LaunchViewModel, router: IRouter) {
        val interactor = LaunchInteractor(App.accountManager, App.pinManager)
        val presenter = LaunchPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
