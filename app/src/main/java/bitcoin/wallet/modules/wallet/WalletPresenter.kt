package bitcoin.wallet.modules.wallet

import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.WalletBalanceItem
import bitcoin.wallet.entities.WalletBalanceViewItem

class WalletPresenter(private var interactor: WalletModule.IInteractor, private val router: WalletModule.IRouter) : WalletModule.IViewDelegate, WalletModule.IInteractorDelegate {

    var view: WalletModule.IView? = null

    override fun viewDidLoad() {
        interactor.notifyWalletBalances()
    }

    override fun didFetchWalletBalances(walletBalances: List<WalletBalanceItem>) {
        var totalBalance = 0.0
        val walletBalanceViewItems = mutableListOf<WalletBalanceViewItem>()

        walletBalances.forEach { balance ->
            totalBalance += balance.coinValue.value * balance.exchangeRate
            walletBalanceViewItems.add(viewItemForBalance(balance))
        }

        walletBalances.firstOrNull()?.currency?.let {
            view?.showTotalBalance(CurrencyValue(it, totalBalance))
        }

        view?.showWalletBalances(walletBalanceViewItems)
    }

    private fun viewItemForBalance(walletBalance: WalletBalanceItem): WalletBalanceViewItem {
        return WalletBalanceViewItem(
                walletBalance.coinValue,
                CurrencyValue(walletBalance.currency, walletBalance.exchangeRate),
                CurrencyValue(walletBalance.currency, walletBalance.coinValue.value * walletBalance.exchangeRate)
        )
    }

}
