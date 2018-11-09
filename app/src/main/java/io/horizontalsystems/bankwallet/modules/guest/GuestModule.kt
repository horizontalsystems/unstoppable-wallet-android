package io.horizontalsystems.bankwallet.modules.guest

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute

object GuestModule {

    interface IView {
        fun showError()
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
        val interactor = GuestInteractor(App.wordsManager, keystoreSafeExecute)
        val presenter = GuestPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
