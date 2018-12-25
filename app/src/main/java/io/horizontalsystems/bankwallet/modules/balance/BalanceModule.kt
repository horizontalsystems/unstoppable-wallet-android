package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.Maybe

object BalanceModule {

    interface IView {
        fun setTitle(title: Int)
        fun didRefresh()
        fun show(totalBalance: CurrencyValue?)
        fun show(wallets: List<BalanceViewItem>)
        fun show(syncStatus: String)
        fun updateBalanceColor(i: Int)
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun refresh()
        fun onReceive(coin: String)
        fun onPay(coin: String)
        fun openManageCoins()
    }

    interface IInteractor {
        fun loadWallets()
        fun refresh()
        fun rate(coin: String): Maybe<Rate>
        val baseCurrency: Currency
        val wallets: List<Wallet>
    }

    interface IInteractorDelegate {
        fun didUpdate()
        fun didRefresh()
    }

    interface IRouter {
        fun openReceiveDialog(coin: String)
        fun openSendDialog(coin: String)
        fun openManageCoins()
    }

    fun init(view: BalanceViewModel, router: IRouter) {
        val interactor = BalanceInteractor(App.walletManager, App.rateManager, App.currencyManager)
        val presenter = BalancePresenter(interactor, router)

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }

}
