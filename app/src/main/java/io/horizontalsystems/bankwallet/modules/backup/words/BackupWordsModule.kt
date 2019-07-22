package io.horizontalsystems.bankwallet.modules.backup.words

import androidx.appcompat.app.AppCompatActivity
import io.horizontalsystems.bankwallet.core.App
import java.util.*

object BackupWordsModule {

    interface IView {
        fun showWords(words: List<String>)
        fun showConfirmationWords(indexes: List<Int>)
        fun showConfirmationError()

        fun loadPage(page: Int)
        fun validateWords()
    }

    interface IPresenter : IInteractorDelegate, IViewDelegate {
        var view: IView?
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onNextClick()
        fun onBackClick()
        fun validateDidClick(confirmationWords: HashMap<Int, String>)
    }

    interface IInteractor {
        fun getConfirmationIndices(): List<Int>
        fun validate(confirmationWords: HashMap<Int, String>)
    }

    interface IInteractorDelegate {
        fun onValidateSuccess()
        fun onValidateFailure()
    }

    interface IRouter {
        fun notifyBackedUp(accountId: String)
        fun close()
    }

    //  helpers

    fun start(context: AppCompatActivity, words: List<String>, accountId: String) {
        BackupWordsActivity.start(context, words, accountId)
    }

    fun init(view: BackupWordsViewModel, router: IRouter, accountId: String, words: List<String>) {
        val interactor = BackupWordsInteractor(App.randomManager, words)
        val presenter = BackupWordsPresenter(interactor, router, State(words, accountId))

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    class State(val words: List<String>, val accountId: String) {

        var currentPage: Int = 1
            private set

        private val pagesCount: Int = 2

        fun canLoadNextPage(): Boolean {
            if (currentPage < pagesCount) {
                currentPage++
                return true
            }
            return false
        }

        fun canLoadPrevPage(): Boolean {
            if (currentPage > 1) {
                currentPage--
                return true
            }
            return false
        }
    }

}
