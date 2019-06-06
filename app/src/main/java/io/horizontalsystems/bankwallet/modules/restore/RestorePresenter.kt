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

    override fun didValidate(words: List<String>) {
        router.navigateToSetSyncMode(words)
    }

}
