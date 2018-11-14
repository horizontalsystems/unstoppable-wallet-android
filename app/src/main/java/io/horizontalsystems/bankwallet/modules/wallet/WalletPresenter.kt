package io.horizontalsystems.bankwallet.modules.wallet

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper

class WalletPresenter(
        private var interactor: WalletModule.IInteractor,
        private val router: WalletModule.IRouter) : WalletModule.IViewDelegate, WalletModule.IInteractorDelegate {

    var view: WalletModule.IView? = null

    override fun onReceive(coin: String) {
        router.openReceiveDialog(coin)
    }

    override fun onPay(coin: String) {
        router.openSendDialog(coin)
    }

    override fun viewDidLoad() {
        view?.setTitle(R.string.wallet_title)
        updateView()
    }

    override fun refresh() {
        interactor.refresh()
    }

    override fun didUpdate() {
        updateView()
    }

    override fun didRefresh() {
        view?.didRefresh()
    }

    private fun updateView() {
        var totalBalance = 0.0

        val viewItems = mutableListOf<WalletViewItem>()
        val currency = interactor.baseCurrency

        var allSynced = true

        interactor.wallets.forEach { wallet ->
            val balance = wallet.adapter.balance
            val rate = interactor.rate(wallet.coin)

            var rateExpired = false

            rate?.let { mRate ->
                val diff = DateHelper.getSecondsAgo(mRate.timestamp)
                rateExpired = diff > 60 * 10

                totalBalance += balance * mRate.value
            }

            viewItems.add(WalletViewItem(
                    coinValue = CoinValue(coin = wallet.coin, value = balance),
                    exchangeValue = rate?.let { CurrencyValue(currency = currency, value = it.value) },
                    currencyValue = rate?.let { CurrencyValue(currency = currency, value = balance * it.value) },
                    state = wallet.adapter.state,
                    rateExpired = rateExpired
            ))

            if (AdapterState.Syncing() == wallet.adapter.state) {
                allSynced = false
            }

            allSynced = allSynced && rate != null
        }

        view?.show(totalBalance = if (allSynced) CurrencyValue(currency = currency, value = totalBalance) else null)
        view?.show(wallets = viewItems)
    }

}
