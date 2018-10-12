package bitcoin.wallet.modules.backup

import android.content.Context
import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.core.managers.Factory
import java.util.*

object BackupModule {

    interface IView {
        fun showWords(words: List<String>)
        fun showConfirmationWithIndexes(indexes: List<Int>)
        fun hideWords()
        fun hideConfirmation()
        fun showConfirmationError()
    }

    interface IViewDelegate {
        fun laterDidClick()
        fun showWordsDidClick()
        fun hideWordsDidClick()
        fun showConfirmationDidClick()
        fun hideConfirmationDidClick()
        fun validateDidClick(confirmationWords: HashMap<Int, String>)
    }

    interface IInteractor {
        fun fetchWords()
        fun fetchConfirmationIndexes()
        fun validate(confirmationWords: HashMap<Int, String>)
    }

    interface IInteractorDelegate {
        fun didFetchWords(words: List<String>)
        fun didFetchConfirmationIndexes(indexes: List<Int>)
        fun didValidateSuccess()
        fun didValidateFailure()
    }

    interface IRouter {
        fun navigateToMain()
        fun close()
    }

    // helpers

    fun start(context: Context, dismissMode: BackupPresenter.DismissMode) {
        BackupActivity.start(context, dismissMode)
    }

    fun init(view: BackupViewModel, router: IRouter, keystoreSafeExecute: IKeyStoreSafeExecute, dismissMode: BackupPresenter.DismissMode) {
        val interactor = BackupInteractor(Factory.wordsManager, Factory.randomProvider, keystoreSafeExecute)
        val presenter = BackupPresenter(interactor, router, dismissMode)

        presenter.view = view

        interactor.delegate = presenter

        view.delegate = presenter
    }

}
