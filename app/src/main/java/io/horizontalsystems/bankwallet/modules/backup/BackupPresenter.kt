package io.horizontalsystems.bankwallet.modules.backup

import java.util.*

    class BackupPresenter(private val interactor: BackupModule.IInteractor, private val router: BackupModule.IRouter, private val dismissMode: DismissMode) : BackupModule.IViewDelegate, BackupModule.IInteractorDelegate {

    enum class DismissMode {
        TO_MAIN, DISMISS_SELF
    }

    var view: BackupModule.IView? = null

    // view delegate

    override fun laterDidClick() {
        dismiss()
    }

    override fun showWordsDidClick() {
        interactor.fetchWords()
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

    // interactor delegate

    override fun didFetchWords(words: List<String>) {
        view?.showWords(words)
    }

    override fun didFetchConfirmationIndexes(indexes: List<Int>) {
        view?.showConfirmationWithIndexes(indexes)
    }

    override fun didValidateSuccess() {
        dismiss()
    }

    override fun didValidateFailure() {
        view?.showConfirmationError()
    }

    private fun dismiss() = when (dismissMode) {
        DismissMode.TO_MAIN -> router.navigateToMain()
        DismissMode.DISMISS_SELF -> router.close()
    }

}
