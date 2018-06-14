package bitcoin.wallet.modules.guest

import android.content.Context
import android.content.Intent
import bitcoin.wallet.lib.WalletDataManager

object GuestModule {

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
        fun openRestoreWalletScreen()
    }

    interface IInteractorDelegate {
        fun didCreateWallet()
    }

    fun start(context: Context) {
        val intent = Intent(context, GuestActivity::class.java)
        context.startActivity(intent)
    }

    fun init(view: IView, router: IRouter) {
        val interactor = GuestInteractor()
        val presenter = GuestPresenter()

        view.presenter = presenter

        interactor.delegate = presenter
        interactor.walletDataProvider = WalletDataManager

        presenter.interactor = interactor
        presenter.view = view
        presenter.router = router
    }

}

