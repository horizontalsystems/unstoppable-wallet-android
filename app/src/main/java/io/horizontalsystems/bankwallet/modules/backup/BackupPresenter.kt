package io.horizontalsystems.bankwallet.modules.backup

import io.horizontalsystems.bankwallet.core.Account
import io.horizontalsystems.bankwallet.core.AccountType
import java.util.*

class BackupPresenter(
        private val interactor: BackupModule.IInteractor,
        private val router: BackupModule.IRouter,
        private val accountId: String)
    : BackupModule.IPresenter, BackupModule.IViewDelegate, BackupModule.IInteractorDelegate {

    private var words = listOf<String>()
    private val steps = BackupWordsSteps()

    //  View

    override var view: BackupModule.IView? = null

    //  View delegate

    override fun viewDidLoad() {
        interactor.getAccount(accountId)
        loadPage()
    }

    override fun onNextClick() {
        if (steps.canLoadNextPage()) {
            when (steps.currentPage) {
                1 -> view?.showWords(words)
                2 -> view?.showConfirmationWords(interactor.fetchConfirmationIndexes())
            }
            loadPage()
        } else {
            view?.validateWords()
        }
    }

    override fun onBackClick() {
        if (steps.canLoadPrevPage()) {
            loadPage()
        } else {
            router.close()
        }
    }

    override fun validateDidClick(confirmationWords: HashMap<Int, String>) {
        if (BackupWordsValidator.isValid(confirmationWords, words)) {
            interactor.setBackedUp(accountId)
            router.close()
        } else {
            view?.showConfirmationError()
        }
    }

    // Interactor Delegate

    override fun onGetAccount(account: Account) {
        when (account.type) {
            is AccountType.Mnemonic -> {
                words = account.type.words
            }
            else -> router.close()
        }

    }

    override fun onGetAccountFailed() {
        router.close()
    }

    // Private

    private fun loadPage() {
        view?.loadPage(steps.currentPage)
    }
}

