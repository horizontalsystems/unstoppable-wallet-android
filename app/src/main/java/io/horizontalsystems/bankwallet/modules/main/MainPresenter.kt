package io.horizontalsystems.bankwallet.modules.main

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.RateUsType
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.core.IPinComponent

class MainPresenter(private val pinComponent: IPinComponent,
                    private val interactor: MainModule.IInteractor) : MainModule.IViewDelegate, MainModule.IInteractorDelegate {

    var view: MainModule.IView? = null
    private var contentHidden = App.pinComponent.isLocked

    override fun viewDidLoad() {
        interactor.onStart()
        updateBadgeVisibility()
    }

    override fun showRateApp(showRateUs: RateUsType) {
        when (showRateUs) {
            RateUsType.OpenPlayMarket -> view?.openPlayMarket()
            RateUsType.ShowDialog -> view?.showRateApp()
        }
    }

    override fun onResume() {
        if (contentHidden != pinComponent.isLocked){
            view?.hideContent(pinComponent.isLocked)
        }
        contentHidden = pinComponent.isLocked
    }

    override fun updateBadgeVisibility() {
        val visible = !(interactor.allBackedUp && interactor.termsAccepted && interactor.isPinSet)
        view?.toggleBagdeVisibility(visible)
    }

    override fun sync(accounts: List<Account>) {
        view?.setTransactionTabEnabled(accounts.isNotEmpty())
    }

    override fun onClear() {
        interactor.clear()
    }
}
