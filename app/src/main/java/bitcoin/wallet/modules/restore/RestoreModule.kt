package bitcoin.wallet.modules.restore

import android.content.Context
import android.content.Intent
import bitcoin.wallet.core.App
import bitcoin.wallet.core.IKeyStoreSafeExecute

object RestoreModule {

    interface IView {
        fun showInvalidWordsError()
    }

    interface IViewDelegate {
        fun restoreDidClick(words: List<String>)
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
        val interactor = RestoreInteractor(App.wordsManager, App.adapterManager, keystoreSafeExecute)
        val presenter = RestorePresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
