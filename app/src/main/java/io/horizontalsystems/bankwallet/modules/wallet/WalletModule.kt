package io.horizontalsystems.bankwallet.modules.wallet

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.ISecuredStorage
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.coins.Coin
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
        val adapterManager = App.adapterManager
        val exchangeRateManager = App.exchangeRateManager
        val securedStorage: ISecuredStorage= App.secureStorage

        val interactor = WalletInteractor(adapterManager, exchangeRateManager, securedStorage)
        val presenter = WalletPresenter(interactor, router)

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }

}
