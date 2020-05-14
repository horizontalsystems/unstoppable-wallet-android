package io.horizontalsystems.bankwallet.modules.main

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.core.IPinComponent

class MainPresenter(private val pinComponent: IPinComponent,
                    private val interactor: MainModule.IInteractor) : MainModule.IViewDelegate, MainModule.IInteractorDelegate {

    var view: MainModule.IView? = null
    private var contentHidden = App.pinComponent.isLocked

    override fun viewDidLoad() {
        interactor.onStart()
    }

    override fun didShowRateApp() {
        view?.showRateApp()
    }

    override fun onResume() {
        if (contentHidden != pinComponent.isLocked){
            view?.hideContent(pinComponent.isLocked)
        }
        contentHidden = pinComponent.isLocked
    }

    override fun onClear() {
        interactor.clear()
    }
}
