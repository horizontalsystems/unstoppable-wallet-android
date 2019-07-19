package io.horizontalsystems.bankwallet.modules.welcome

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App

object WelcomeModule {

    interface IView {
        fun showError()
        fun setAppVersion(appVersion: String)
    }

    interface IViewDelegate {
        fun createWalletDidClick()
        fun restoreWalletDidClick()
        fun viewDidLoad()
    }

    interface IInteractor {
        fun createWallet()
        val appVersion: String
    }

    interface IInteractorDelegate {
        fun didCreateWallet()
        fun didFailToCreateWallet()
    }

    interface IRouter {
        fun openMainModule()
        fun openRestoreModule()
    }

    fun start(context: Context) {
        start(context, false)
    }

    fun startAsNewTask(context: Context) {
        start(context, true)
    }

    private fun start(context: Context, newTask: Boolean = false) {
        val intent = Intent(context, WelcomeActivity::class.java)
        if (newTask) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }

    fun init(view: WelcomeViewModel, router: IRouter) {
        val interactor = WelcomeInteractor(App.predefinedAccountTypeManager, App.systemInfoManager)
        val presenter = WelcomePresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
