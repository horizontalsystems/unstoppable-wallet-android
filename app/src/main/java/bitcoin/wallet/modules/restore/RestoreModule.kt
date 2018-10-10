package bitcoin.wallet.modules.restore

import android.content.Context
import android.content.Intent
import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.managers.Factory

object RestoreModule {

    interface IView {
        fun showInvalidWordsError()
        fun authenticateToRestoreWallet()
    }

    interface IViewDelegate {
        fun restoreDidClick(words: List<String>)
    }

    interface IKeyStoreSafeExecute {
        fun safeExecute(action: Runnable, onSuccess: Runnable? = null, onFailure: Runnable? = null)
    }

    interface IInteractor {
        fun restore(words: List<String>)
    }

    interface IInteractorDelegate {
        fun didRestore()
        fun didFailToRestore()
    }

    interface IRouter {
        fun navigateToMain()
    }

    fun start(context: Context) {
        val intent = Intent(context, RestoreWalletActivity::class.java)
        context.startActivity(intent)
    }

    fun init(view: RestoreViewModel, router: IRouter, keystoreSafeExecute: IKeyStoreSafeExecute) {
        val interactor = RestoreInteractor(Factory.wordsManager, AdapterManager, keystoreSafeExecute)
        val presenter = RestorePresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
