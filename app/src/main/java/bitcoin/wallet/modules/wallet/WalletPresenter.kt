package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.DollarCurrency
import io.reactivex.subjects.BehaviorSubject

class WalletPresenter(private var interactor: WalletModule.IInteractor, private val router: WalletModule.IRouter) : WalletModule.IViewDelegate, WalletModule.IInteractorDelegate {

    var view: WalletModule.IView? = null

    private var coinValues = mutableMapOf<String, CoinValue>()
    private var rates = mutableMapOf<String, Double>()
    private var progresses = mutableMapOf<String, BehaviorSubject<Double>>()
    var currency: Currency = DollarCurrency()

    override fun onReceiveClicked(adapterId: String) {
        router.openReceiveDialog(adapterId)
    }

    override fun onSendClicked(adapterId: String) {
        val adapter = AdapterManager.adapters.firstOrNull { it.id == adapterId }
        adapter?.let { router.openSendDialog(it) }
    }

    override fun viewDidLoad() {
        interactor.notifyWalletBalances()
    }

    override fun didInitialFetch(coinValues: MutableMap<String, CoinValue>, rates: MutableMap<String, Double>, progresses: MutableMap<String, BehaviorSubject<Double>>, currency: Currency) {
        this.coinValues = coinValues
        this.rates = rates
        this.progresses = progresses
        this.currency = currency

        updateView()
    }

    override fun didUpdate(coinValue: CoinValue, adapterId: String) {
        coinValues[adapterId] = coinValue

        updateView()
    }

    override fun didUpdate(rates: MutableMap<String, Double>) {
        this.rates = rates

        updateView()
    }

    private fun updateView() {
        var totalBalance = 0.0
        val viewItems = mutableListOf<WalletBalanceViewItem>()

        for (item in coinValues) {

            val adapterId = item.key
            val coinValue = item.value
            val rate = rates[coinValue.coin.code] ?: 0.0

            viewItems.add(
                    WalletBalanceViewItem(
                            adapterId = adapterId,
                            coinValue = coinValue,
                            exchangeValue = CurrencyValue(currency, rate),
                            currencyValue = CurrencyValue(currency, coinValue.value * rate),
                            progress = progresses[adapterId]
                    )
            )

            totalBalance += coinValue.value * rate
        }

        view?.showTotalBalance(CurrencyValue(currency, totalBalance))
        view?.showWalletBalances(viewItems)
    }

}
