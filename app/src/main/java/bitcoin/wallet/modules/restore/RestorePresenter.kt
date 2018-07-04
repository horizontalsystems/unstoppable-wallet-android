package bitcoin.wallet.modules.restore

import android.security.keystore.UserNotAuthenticatedException

class RestorePresenter(private val interactor: RestoreModule.IInteractor, private val router: RestoreModule.IRouter) : RestoreModule.IViewDelegate, RestoreModule.IInteractorDelegate {

    var view: RestoreModule.IView? = null

    override fun restoreDidClick(words: List<String>) {
        interactor.restore(words)
    }

    override fun cancelDidClick() {
        router.close()
    }

    override fun didRestore() {
        router.navigateToMain()
    }

    override fun didFailToRestore(error: Throwable) {

        if (error is UserNotAuthenticatedException) {
            view?.authenticateToRestoreWallet()
        } else {
            view?.showInvalidWordsError()
        }
    }

}
