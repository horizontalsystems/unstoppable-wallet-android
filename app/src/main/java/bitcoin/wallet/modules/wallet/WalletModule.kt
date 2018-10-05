package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.IAdapter
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.coins.Coin
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
        fun checkIfPinSet()
    }

    interface IInteractorDelegate {
        fun didInitialFetch(coinValues: MutableMap<String, CoinValue>, rates: MutableMap<Coin, CurrencyValue>, progresses: MutableMap<String, BehaviorSubject<Double>>)
        fun didUpdate(coinValue: CoinValue, adapterId: String)
        fun didExchangeRateUpdate(rates: MutableMap<Coin, CurrencyValue>)
        fun onPinNotSet()
    }

    interface IRouter {
        fun openReceiveDialog(adapterId: String)
        fun openSendDialog(adapter: IAdapter)
        fun navigateToSetPin()
    }

    fun init(view: WalletViewModel, router: IRouter) {
        val adapterManager = AdapterManager
        val exchangeRateManager = Factory.exchangeRateManager
        val storage: ILocalStorage = Factory.preferencesManager

        val interactor = WalletInteractor(adapterManager, exchangeRateManager, storage)
        val presenter = WalletPresenter(interactor, router)

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }

}
