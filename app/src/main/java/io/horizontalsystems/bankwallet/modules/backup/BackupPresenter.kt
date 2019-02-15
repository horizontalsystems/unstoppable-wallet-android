package io.horizontalsystems.bankwallet.modules.backup

import java.util.*

class BackupPresenter(
        private val interactor: BackupModule.IInteractor,
        private val router: BackupModule.IRouter,
        private val dismissMode: DismissMode,
        private val state: BackupModule.BackupModuleState) : BackupModule.IViewDelegate, BackupModule.IInteractorDelegate {

    enum class DismissMode {
        SET_PIN, DISMISS_SELF
    }

    var view: BackupModule.IView? = null

    // view delegate

    override fun viewDidLoad() {
        loadPage()
    }

    override fun onNextClick() {
        if (state.canLoadNextPage()) {
            when {
                state.currentPage == 1 -> interactor.fetchWords()
                state.currentPage == 2 -> interactor.fetchConfirmationIndexes()
            }
            loadPage()
        } else {
            view?.validateWords()
        }
    }

    override fun onBackClick() {
        if (state.canLoadPrevPage()) {
            loadPage()
        } else {
            dismissOrShowConfirmationDialog()
        }
    }

    override fun validateDidClick(confirmationWords: HashMap<Int, String>) {
        interactor.validate(confirmationWords)
    }

    override fun onTermsConfirm() {
        interactor.onTermsConfirm()
        dismiss()
    }

    // interactor delegate

    override fun didFetchWords(words: List<String>) {
        view?.showWords(words)
    }

    override fun didFetchConfirmationIndexes(indexes: List<Int>) {
        view?.showConfirmationWords(indexes)
    }

    override fun didValidateSuccess() {
        dismissOrShowConfirmationDialog()
    }

    override fun didValidateFailure() {
        view?.showConfirmationError()
    }

    private fun dismissOrShowConfirmationDialog() {
        if (interactor.shouldShowTermsConfirmation()) {
            view?.showTermsConfirmDialog()
        } else {
            dismiss()
        }
    }

    private fun loadPage() {
        view?.loadPage(state.currentPage)
    }

    private fun dismiss() = when (dismissMode) {
        DismissMode.SET_PIN -> router.navigateToSetPin()
        DismissMode.DISMISS_SELF -> router.close()
    }

}
