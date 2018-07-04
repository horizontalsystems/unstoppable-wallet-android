package bitcoin.wallet.modules.guest

import android.content.Context
import android.content.Intent
import bitcoin.wallet.core.managers.Factory

object GuestModule {

    interface IView {
        fun showError()
        fun authenticateToCreateWallet()
    }

    interface IViewDelegate {
        fun createWalletDidClick()
        fun restoreWalletDidClick()
    }

    interface IInteractor {
        fun createWallet()
    }

    interface IInteractorDelegate {
        fun didCreateWallet()
        fun didFailToCreateWallet(error: Throwable)
    }

    interface IRouter {
        fun navigateToBackupRoutingToMain()
        fun navigateToRestore()
    }

    fun start(context: Context) {
        val intent = Intent(context, GuestActivity::class.java)
        context.startActivity(intent)
    }

    fun init(view: GuestViewModel, router: IRouter) {
        val interactor = GuestInteractor(Factory.mnemonicManager, Factory.loginManager)
        val presenter = GuestPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}

