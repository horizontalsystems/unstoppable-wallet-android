package bitcoin.wallet.modules.guest

import android.content.Context
import android.content.Intent
import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.managers.Factory

object GuestModule {

    interface IView {
        fun authenticateToCreateWallet()
        fun showError()
    }

    interface IViewDelegate {
        fun createWalletDidClick()
        fun restoreWalletDidClick()
    }

    interface IKeyStoreSafeExecute {
        fun safeExecute(action: Runnable, onSuccess: Runnable? = null, onFailure: Runnable? = null)
    }

    interface IInteractor {
        fun createWallet()
    }

    interface IInteractorDelegate {
        fun didCreateWallet()
        fun didFailToCreateWallet()
    }

    interface IRouter {
        fun navigateToBackupRoutingToMain()
        fun navigateToRestore()
    }

    fun start(context: Context) {
        val intent = Intent(context, GuestActivity::class.java)
        context.startActivity(intent)
    }

    fun init(view: GuestViewModel, router: IRouter, keystoreSafeExecute: IKeyStoreSafeExecute) {
        val interactor = GuestInteractor(Factory.wordsManager, AdapterManager, keystoreSafeExecute)
        val presenter = GuestPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}

