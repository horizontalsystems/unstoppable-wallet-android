package io.horizontalsystems.bankwallet.modules.restore.eos

class RestoreEosPresenter(private val interactor: RestoreEosModule.IInteractor, private val router: RestoreEosModule.IRouter)
    : RestoreEosModule.IViewDelegate, RestoreEosModule.IInteractorDelegate {

    var view: RestoreEosModule.IView? = null

    //  ViewDelegate

    override fun onClickDone(accountName: String, privateKey: String) {
        interactor.validate(accountName, privateKey)
    }

    //  InteractorDelegate

    override fun onValidationSuccess(accountName: String, privateKey: String) {
        router.finishWithSuccess(accountName, privateKey)
    }

    override fun onValidationFail(error: Exception) {
        TODO("not implemented")
    }
}
