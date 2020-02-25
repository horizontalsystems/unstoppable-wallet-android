package io.horizontalsystems.bankwallet.modules.welcome

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App

object WelcomeModule {

    interface IView {
        fun setAppVersion(appVersion: String)
    }

    interface IViewDelegate {
        fun createWalletDidClick()
        fun restoreWalletDidClick()
        fun viewDidLoad()
        fun openTorPage()
    }

    interface IInteractor {
        val appVersion: String
    }

    interface IRouter {
        fun openRestoreModule()
        fun openCreateWalletModule()
        fun openTorPage()
    }

    fun start(context: Context) {
        val intent = Intent(context, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        context.startActivity(intent)
    }

    fun init(view: WelcomeViewModel, router: IRouter) {
        val interactor = WelcomeInteractor(App.systemInfoManager)
        val presenter = WelcomePresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
    }

}
