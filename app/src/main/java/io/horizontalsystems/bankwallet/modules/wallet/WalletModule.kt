package io.horizontalsystems.bankwallet.modules.wallet

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.Single

object WalletModule {

    interface IView {
        fun setTitle(title: Int)
        fun didRefresh()
        fun show(totalBalance: CurrencyValue?)
        fun show(wallets: List<WalletViewItem>)
        fun show(syncStatus: String)
        fun updateBalanceColor(i: Int)
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun refresh()
        fun onReceive(coin: String)
        fun onPay(coin: String)
    }

    interface IInteractor {
        fun loadWallets()
        fun refresh()
        fun rate(coin: String): Single<Rate>
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
    }

    fun init(view: WalletViewModel, router: IRouter) {
        val interactor = WalletInteractor(App.walletManager, App.rateManager, App.currencyManager)
        val presenter = WalletPresenter(interactor, router)

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }

}
