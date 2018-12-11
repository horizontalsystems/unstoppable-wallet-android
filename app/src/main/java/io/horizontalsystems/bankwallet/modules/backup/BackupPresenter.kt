package io.horizontalsystems.bankwallet.modules.backup

import java.util.*

class BackupPresenter(private val interactor: BackupModule.IInteractor, private val router: BackupModule.IRouter, private val dismissMode: DismissMode) : BackupModule.IViewDelegate, BackupModule.IInteractorDelegate {

    enum class DismissMode {
        SET_PIN, DISMISS_SELF
    }

    var view: BackupModule.IView? = null

    // view delegate

    override fun showWordsDidClick() {
        router.openWordsListScreen()
    }

    override fun hideWordsDidClick() {
        view?.hideWords()
    }

    override fun showConfirmationDidClick() {
        interactor.fetchConfirmationIndexes()
    }

    override fun hideConfirmationDidClick() {
        view?.hideConfirmation()
    }

    override fun validateDidClick(confirmationWords: HashMap<Int, String>) {
        interactor.validate(confirmationWords)
    }

    override fun onTermsConfirm() {
        interactor.onTermsConfirm()
        dismiss()
    }

    override fun onLaterClick() {
        dismissOrShowConfirmationDialog()
    }

    override fun wordsListViewLoaded() {
        interactor.fetchWords()
    }

    // interactor delegate

    override fun didFetchWords(words: List<String>) {
        view?.showWords(words)
    }

    override fun didFetchConfirmationIndexes(indexes: List<Int>) {
        view?.showConfirmationWithIndexes(indexes)
    }

    override fun didValidateSuccess() {
        dismissOrShowConfirmationDialog()
    }

    private fun dismissOrShowConfirmationDialog() {
        if (interactor.shouldShowTermsConfirmation()) {
            view?.showTermsConfirmDialog()
        } else {
            dismiss()
        }
    }

    override fun didValidateFailure() {
        view?.showConfirmationError()
    }

    private fun dismiss() = when (dismissMode) {
        DismissMode.SET_PIN -> router.navigateToSetPin()
        DismissMode.DISMISS_SELF -> router.close()
    }

}
