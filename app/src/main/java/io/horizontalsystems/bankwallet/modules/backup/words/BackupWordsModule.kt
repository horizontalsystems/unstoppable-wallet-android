package io.horizontalsystems.bankwallet.modules.backup.words

import io.horizontalsystems.bankwallet.core.managers.RandomProvider
import java.util.*

object BackupWordsModule {
    const val requestKey = "key_backup_words"
    const val requestResult = "key_backup_result"
    const val RESULT_BACKUP = 1
    const val RESULT_SHOW = 2

    interface IView {
        fun showWords(words: Array<String>)
        fun showConfirmationWords(indexes: List<Int>)
        fun showConfirmationError()

        fun loadPage(page: Int)
        fun setBackedUp(backedUp: Boolean)
        fun showAdditionalInfo(additionalInfo: String?)
    }

    interface IPresenter : IInteractorDelegate, IViewDelegate {
        var view: IView?
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onNextClick()
        fun onBackClick()
        fun onCloseClick()
        fun validateDidClick(confirmationWords: HashMap<Int, String>)
    }

    interface IInteractor {
        fun getConfirmationIndices(maxIndex: Int): List<Int>
        fun validate(confirmationWords: HashMap<Int, String>)
    }

    interface IInteractorDelegate {
        fun onValidateSuccess()
        fun onValidateFailure()
    }

    interface IRouter {
        fun notifyBackedUp()
        fun notifyClosed()
        fun close()
    }

    //  helpers

    fun init(view: BackupWordsViewModel, router: IRouter, words: Array<String>, backedUp: Boolean, additionalInfo: String?) {
        val interactor = BackupWordsInteractor(RandomProvider(), words)
        val presenter = BackupWordsPresenter(interactor, router, State(words, backedUp), additionalInfo)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    class State(val words: Array<String>, val backedUp: Boolean) {

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
