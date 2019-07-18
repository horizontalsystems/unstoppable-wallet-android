package io.horizontalsystems.bankwallet.modules.restore.words

import io.horizontalsystems.bankwallet.R

class RestoreWordsPresenter(private val interactor: RestoreWordsModule.IInteractor, private val router: RestoreWordsModule.IRouter)
    : RestoreWordsModule.IViewDelegate, RestoreWordsModule.IInteractorDelegate {

    var view: RestoreWordsModule.IView? = null

    // View Delegate

    override fun restoreDidClick(words: List<String>) {
        interactor.validate(words)
    }

    // Interactor Delegate

    override fun didFailToValidate(exception: Exception) {
        view?.showError(R.string.Restore_ValidationFailed)
    }

    override fun didValidate(words: List<String>) {
        router.navigateToSetSyncMode(words)
    }

}
