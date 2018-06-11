package org.grouvi.wallet.modules.wallet

import android.arch.lifecycle.ViewModel

object WalletModule {

    interface IView {
        var presenter: IPresenter
    }

    interface IPresenter {
        fun start()

        var view: IView
        var interactor: IInteractor
        var router: IRouter
    }

    interface IInteractor {
        var delegate: IInteractorDelegate
    }

    interface IRouter

    interface IInteractorDelegate


    fun init(view: IView, router: IRouter) {

        val presenter = WalletModulePresenter()

        presenter.view = view
        presenter.router = router

        view.presenter = presenter
    }

}


class WalletViewModel : ViewModel(), WalletModule.IView, WalletModule.IRouter {
    override lateinit var presenter: WalletModule.IPresenter

    fun init() {
        WalletModule.init(this, this)

        presenter.start()
    }

}

class WalletModulePresenter : WalletModule.IPresenter {

    override lateinit var view: WalletModule.IView
    override lateinit var interactor: WalletModule.IInteractor
    override lateinit var router: WalletModule.IRouter

    override fun start() {
    }
}