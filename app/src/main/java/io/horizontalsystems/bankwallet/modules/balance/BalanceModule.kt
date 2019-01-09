package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

object BalanceModule {

    interface IView {
        fun didRefresh()
        fun reload()
        fun updateItem(position: Int)
        fun updateHeader()
    }

    interface IViewDelegate {
        val itemsCount: Int

        fun viewDidLoad()
        fun getViewItem(position: Int): BalanceViewItem
        fun getHeaderViewItem(): BalanceHeaderViewItem
        fun refresh()
        fun onReceive(position: Int)
        fun onPay(position: Int)
        fun openManageCoins()
    }

    interface IInteractor {
        fun refresh()
        fun initWallets()
        fun fetchRates(currencyCode: String, coinCodes: List<CoinCode>)
    }

    interface IInteractorDelegate {
        fun didUpdateWallets(wallets: List<Wallet>)
        fun didUpdateCurrency(currency: Currency)
        fun didUpdateBalance(coinCode: CoinCode, balance: Double)
        fun didUpdateState(coinCode: String, state: AdapterState)
        fun didUpdateRate(rate: Rate)
        fun didRefresh()
    }

    interface IRouter {
        fun openReceiveDialog(coin: String)
        fun openSendDialog(coin: String)
        fun openManageCoins()
    }

    class BalanceItemDataSource {
        val count
            get() = items.count()

        var currency: Currency? = null

        val coinCodes: List<CoinCode>
            get() = items.map { it.coinCode }

        var items = listOf<BalanceItem>()

        fun getItem(position: Int): BalanceItem = items[position]

        fun reset(items: List<BalanceItem>) {
            this.items = items
        }

        fun getPosition(coinCode: CoinCode): Int {
            return items.indexOfFirst { it.coinCode == coinCode }
        }

        fun setBalance(position: Int, balance: Double) {
            items[position].balance = balance
        }

        fun setState(position: Int, state: AdapterState) {
            items[position].state = state
        }

        fun setRate(position: Int, rate: Rate) {
            items[position].rate = rate
        }

        fun clearRates() {
            items.forEach {
                it.rate = null
            }
        }
    }

    data class BalanceItem(
            val title: String,
            val coinCode: CoinCode,
            var balance: Double = 0.0,
            var state: AdapterState = AdapterState.NotSynced,
            var rate: Rate? = null
    )

    fun init(view: BalanceViewModel, router: IRouter) {
        val interactor = BalanceInteractor(App.walletManager, App.rateStorage, App.currencyManager)
        val presenter = BalancePresenter(interactor, router, BalanceItemDataSource(), BalanceViewItemFactory())

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }

}
