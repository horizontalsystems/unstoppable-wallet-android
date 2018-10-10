package bitcoin.wallet.modules.restore

class RestorePresenter(private val interactor: RestoreModule.IInteractor, private val router: RestoreModule.IRouter) : RestoreModule.IViewDelegate, RestoreModule.IInteractorDelegate {

    var view: RestoreModule.IView? = null

    override fun restoreDidClick(words: List<String>) {
        interactor.restore(words)
    }

    override fun didRestore() {
        router.navigateToMain()
    }

    override fun didFailToRestore() {
        view?.showInvalidWordsError()
    }

}
