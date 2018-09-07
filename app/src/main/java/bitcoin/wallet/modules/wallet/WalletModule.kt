package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.IAdapter
import bitcoin.wallet.core.managers.DatabaseManager
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.entities.CurrencyValue
import io.reactivex.subjects.BehaviorSubject

object WalletModule {

    interface IView {
        fun showTotalBalance(totalBalance: CurrencyValue)
        fun showWalletBalances(walletBalances: List<WalletBalanceViewItem>)
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onReceiveClicked(adapterId: String)
        fun onSendClicked(adapterId: String)
    }

    interface IInteractor {
        fun notifyWalletBalances()
    }

    interface IInteractorDelegate {
        fun didInitialFetch(coinValues: MutableMap<String, CoinValue>, rates: MutableMap<String, Double>, progresses: MutableMap<String, BehaviorSubject<Double>>, currency: Currency)
        fun didUpdate(coinValue: CoinValue, adapterId: String)
        fun didUpdate(rates: MutableMap<String, Double>)
    }

    interface IRouter {
        fun openReceiveDialog(adapterId: String)
        fun openSendDialog(adapter: IAdapter)
    }

    private var databaseManager: DatabaseManager? = null

    fun init(view: WalletViewModel, router: IRouter) {
        val databaseManager = Factory.databaseManager
        val adapterManager = AdapterManager

        val interactor = WalletInteractor(adapterManager, databaseManager)
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
