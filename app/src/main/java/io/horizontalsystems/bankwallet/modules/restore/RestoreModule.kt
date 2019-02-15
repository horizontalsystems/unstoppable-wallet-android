package io.horizontalsystems.bankwallet.modules.restore

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute

object RestoreModule {

    interface IView {
        fun showError(error: Int)
        fun showConfirmationDialog()
    }

    interface IViewDelegate {
        fun restoreDidClick(words: List<String>)
        fun didConfirm(words: List<String>)
    }

    interface IInteractor {
        fun restore(words: List<String>)
        fun validate(words: List<String>)
    }

    interface IInteractorDelegate {
        fun didRestore()
        fun didFailToRestore(exception: Exception)
        fun didFailToValidate(exception: Exception)
        fun didValidate()
    }

    interface IRouter {
        fun navigateToSetPin()
    }

    fun start(context: Context) {
        val intent = Intent(context, RestoreWalletActivity::class.java)
        context.startActivity(intent)
    }

    fun init(view: RestoreViewModel, router: IRouter, keystoreSafeExecute: IKeyStoreSafeExecute) {
        val interactor = RestoreInteractor(App.authManager, App.wordsManager, App.localStorage, keystoreSafeExecute)
        val presenter = RestorePresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    class RestoreFailedException : Exception()

}
