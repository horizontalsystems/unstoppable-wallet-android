package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.ExchangeRateManager
import bitcoin.wallet.core.IAdapter
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
        fun didInitialFetch(coinValues: MutableMap<String, CoinValue>, rates: Map<String, Double>, progresses: MutableMap<String, BehaviorSubject<Double>>, currency: Currency)
        fun didUpdate(coinValue: CoinValue, adapterId: String)
        fun didUpdate(rates: Map<String, Double>)
    }

    interface IRouter {
        fun openReceiveDialog(adapterId: String)
        fun openSendDialog(adapter: IAdapter)
    }

    fun init(view: WalletViewModel, router: IRouter) {
        val adapterManager = AdapterManager
        val exchangeRateManager = ExchangeRateManager

        val interactor = WalletInteractor(adapterManager, exchangeRateManager)
        val presenter = WalletPresenter(interactor, router)

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }

}
