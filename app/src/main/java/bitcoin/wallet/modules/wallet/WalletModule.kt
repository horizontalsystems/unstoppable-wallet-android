package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.managers.DatabaseManager
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.WalletBalanceItem
import bitcoin.wallet.entities.WalletBalanceViewItem

object WalletModule {

    interface IView {
        fun showTotalBalance(totalBalance: CurrencyValue)
        fun showWalletBalances(walletBalances: List<WalletBalanceViewItem>)
    }

    interface IViewDelegate {
        fun viewDidLoad()
    }

    interface IInteractor {
        fun notifyWalletBalances()
    }

    interface IInteractorDelegate {
        fun didFetchWalletBalances(walletBalances: List<WalletBalanceItem>)
    }

    interface IRouter

    private var databaseManager: DatabaseManager? = null

    fun init(view: WalletViewModel, router: IRouter) {
        val databaseManager = DatabaseManager()

        val interactor = WalletInteractor(databaseManager)
        val presenter = WalletPresenter(interactor, router)

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter

        this.databaseManager = databaseManager
    }

    fun destroy() {
        databaseManager?.close()
    }

}
