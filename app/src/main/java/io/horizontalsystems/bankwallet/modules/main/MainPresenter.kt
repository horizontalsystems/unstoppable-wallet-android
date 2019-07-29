package io.horizontalsystems.bankwallet.modules.main

class MainPresenter(private val interactor: MainModule.IInteractor,
                    private val router: MainModule.IRouter) : MainModule.IViewDelegate, MainModule.IInteractorDelegate {

    var view: MainModule.IView? = null

    override fun viewDidLoad() {
        interactor.onStart()
    }

}
