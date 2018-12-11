package io.horizontalsystems.bankwallet.modules.backup

import android.content.Context
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import java.util.*

object BackupModule {

    interface IView {
        fun showWords(words: List<String>)
        fun showConfirmationWithIndexes(indexes: List<Int>)
        fun hideWords()
        fun hideConfirmation()
        fun showConfirmationError()
        fun showTermsConfirmDialog()
    }

    interface IViewDelegate {
        fun showWordsDidClick()
        fun hideWordsDidClick()
        fun showConfirmationDidClick()
        fun hideConfirmationDidClick()
        fun validateDidClick(confirmationWords: HashMap<Int, String>)
        fun onTermsConfirm()
        fun onLaterClick()
        fun wordsListViewLoaded()
    }

    interface IInteractor {
        fun fetchWords()
        fun fetchConfirmationIndexes()
        fun validate(confirmationWords: HashMap<Int, String>)
        fun onTermsConfirm()
        fun shouldShowTermsConfirmation(): Boolean
    }

    interface IInteractorDelegate {
        fun didFetchWords(words: List<String>)
        fun didFetchConfirmationIndexes(indexes: List<Int>)
        fun didValidateSuccess()
        fun didValidateFailure()
    }

    interface IRouter {
        fun navigateToSetPin()
        fun close()
    }

    // helpers

    fun start(context: Context, dismissMode: BackupPresenter.DismissMode) {
        BackupActivity.start(context, dismissMode)
    }

    fun init(view: BackupViewModel, router: IRouter, keystoreSafeExecute: IKeyStoreSafeExecute, dismissMode: BackupPresenter.DismissMode) {
        val interactor = BackupInteractor(App.wordsManager, App.randomManager, App.localStorage, keystoreSafeExecute)
        val presenter = BackupPresenter(interactor, router, dismissMode)

        presenter.view = view

        interactor.delegate = presenter

        view.delegate = presenter
    }

}
