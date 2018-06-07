package org.grouvi.wallet.modules.addWallet

import android.content.Context
import android.content.Intent
import org.grouvi.wallet.lib.WalletDataManager

object AddWalletModule {

    interface IView {
        var presenter: IPresenter
    }

    interface IPresenter {
        fun start()
        fun createWallet()
        fun restoreWallet()

        var view: IView
        var interactor: IInteractor
        var router: IRouter
    }

    interface IInteractor {
        fun createWallet()

        var delegate: IInteractorDelegate
        var walletDataProvider: WalletDataManager
    }

    interface IRouter {
        fun openBackupScreen()
    }

    interface IInteractorDelegate {
        fun didCreateWallet()
    }

    fun start(context: Context) {
        val intent = Intent(context, AddWalletActivity::class.java)
        context.startActivity(intent)
    }

    fun init(view: IView, router: IRouter) {
        val interactor = AddWalletModuleInteractor()
        val presenter = AddWalletModulePresenter()

        view.presenter = presenter

        interactor.delegate = presenter
        interactor.walletDataProvider = WalletDataManager

        presenter.interactor = interactor
        presenter.view = view
        presenter.router = router
    }

}

class AddWalletModulePresenter : AddWalletModule.IPresenter, AddWalletModule.IInteractorDelegate {
    override lateinit var view: AddWalletModule.IView
    override lateinit var interactor: AddWalletModule.IInteractor
    override lateinit var router: AddWalletModule.IRouter

    override fun start() {
        // todo: do nothing?
    }

    override fun createWallet() {
        interactor.createWallet()
    }

    override fun restoreWallet() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // interactor delegate

    override fun didCreateWallet() {
        router.openBackupScreen()
    }
}

class AddWalletModuleInteractor: AddWalletModule.IInteractor {

    override lateinit var delegate: AddWalletModule.IInteractorDelegate
    override lateinit var walletDataProvider: WalletDataManager

    override fun createWallet() {
        walletDataProvider.createWallet()

        delegate.didCreateWallet()
    }

}

