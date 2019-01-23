package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.bankwallet.R

class RestorePresenter(
        private val interactor: RestoreModule.IInteractor,
        private val router: RestoreModule.IRouter) : RestoreModule.IViewDelegate, RestoreModule.IInteractorDelegate {

    var view: RestoreModule.IView? = null

    override fun restoreDidClick(words: List<String>) {
        interactor.validate(words)
    }

    override fun didFailToValidate(exception: Exception) {
        view?.showError(R.string.Restore_ValidationFailed)
    }

    override fun didFailToRestore(exception: Exception) {
        view?.showError(R.string.Restore_RestoreFailed)
    }

    override fun didValidate() {
        view?.showConfirmationDialog()
    }

    override fun didConfirm(words: List<String>) {
        interactor.restore(words)
    }

    override fun didRestore() {
        router.navigateToSetPin()
    }
}
