package io.horizontalsystems.bankwallet.modules.backup

import android.content.Context
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import java.util.*

object BackupModule {

    interface IView {
        fun showWords(words: List<String>)
        fun showConfirmationWords(indexes: List<Int>)
        fun showConfirmationError()
        fun showTermsConfirmDialog()

        fun loadPage(page: Int)
        fun validateWords()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onNextClick()
        fun onBackClick()
        fun validateDidClick(confirmationWords: HashMap<Int, String>)
        fun onTermsConfirm()
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
        val interactor = BackupInteractor(App.authManager, App.wordsManager, App.randomManager, App.localStorage, keystoreSafeExecute)
        val presenter = BackupPresenter(interactor, router, dismissMode, BackupModuleState())

        presenter.view = view

        interactor.delegate = presenter

        view.delegate = presenter
    }

    class BackupModuleState {
        private val pagesCount: Int = 3
        var currentPage: Int = 0

        fun canLoadNextPage(): Boolean {
            if ((currentPage + 1) < pagesCount) {
                currentPage++
                return true
            }
            return false
        }
        fun canLoadPrevPage(): Boolean{
            if (currentPage > 0) {
                currentPage--
                return true
            }
            return false
        }
    }

}
