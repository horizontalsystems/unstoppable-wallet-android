package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.hdwalletkit.Mnemonic

class RestorePresenter(
        private val interactor: RestoreModule.IInteractor,
        private val router: RestoreModule.IRouter) : RestoreModule.IViewDelegate, RestoreModule.IInteractorDelegate {

    var view: RestoreModule.IView? = null

    override fun restoreDidClick(words: List<String>) {
        interactor.restore(words)
    }

    override fun didRestore() {
        router.navigateToSetPin()
    }

    override fun didFailToRestore(exception: Exception) {
        when (exception) {
            is RestoreModule.RestoreFailedException -> view?.showError(R.string.Restore_RestoreFailed)
            is Mnemonic.MnemonicException -> view?.showError(R.string.Restore_ValidationFailed)
        }
    }

}
