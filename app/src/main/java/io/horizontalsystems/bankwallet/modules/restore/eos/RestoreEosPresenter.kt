package io.horizontalsystems.bankwallet.modules.restore.eos

class RestoreEosPresenter(private val interactor: RestoreEosInteractor,
                          private val router: RestoreEosModule.IRouter) : RestoreEosModule.IViewDelegate, RestoreEosModule.IInteractorDelegate {

    var view: RestoreEosModule.IView? = null
}