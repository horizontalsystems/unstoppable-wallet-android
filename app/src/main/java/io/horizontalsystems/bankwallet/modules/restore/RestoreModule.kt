package io.horizontalsystems.bankwallet.modules.restore

import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App

object RestoreModule {

    interface IView {
        fun showError(error: Int)
    }

    interface IViewDelegate {
        fun restoreDidClick(words: List<String>)
    }

    interface IInteractor {
        fun validate(words: List<String>)
    }

    interface IInteractorDelegate {
        fun didFailToValidate(exception: Exception)
        fun didValidate(words: List<String>)
    }

    interface IRouter {
        fun navigateToSetSyncMode(words: List<String>)
    }

    fun start(context: Context) {
        val intent = Intent(context, RestoreWalletActivity::class.java)
        context.startActivity(intent)
    }

    fun init(view: RestoreViewModel, router: IRouter) {
        val interactor = RestoreInteractor(App.wordsManager)
        val presenter = RestorePresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    class RestoreFailedException : Exception()

}
