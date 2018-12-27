package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.disposables.CompositeDisposable

class BalancePresenter(
        private var interactor: BalanceModule.IInteractor,
        private val router: BalanceModule.IRouter,
        private val dataSource: BalanceModule.BalanceItemDataSource,
        private val factory: BalanceViewItemFactory) : BalanceModule.IViewDelegate, BalanceModule.IInteractorDelegate {

    private var disposables: CompositeDisposable = CompositeDisposable()

    var view: BalanceModule.IView? = null

    //
    // BalanceModule.IViewDelegate
    //
    override val itemsCount: Int
        get() = dataSource.count


    override fun viewDidLoad() {
        interactor.initWallets()
    }

    override fun getViewItem(position: Int) =
            factory.createViewItem(dataSource.getItem(position), dataSource.currency)

    override fun getHeaderViewItem() =
            factory.createHeaderViewItem(dataSource.items, dataSource.currency)

    override fun refresh() {
        interactor.refresh()
    }

    override fun onReceive(position: Int) {
        router.openReceiveDialog(dataSource.getItem(position).coinCode)
    }

    override fun onPay(position: Int) {
        router.openSendDialog(dataSource.getItem(position).coinCode)
    }

    //
    // BalanceModule.IInteractorDelegate
    //
    override fun didUpdateWallets(wallets: List<Wallet>) {
        dataSource.reset(wallets.map { BalanceModule.BalanceItem(it.title, it.coinCode) })
        dataSource.currency?.let {
            interactor.fetchRates(it.code, dataSource.coinCodes)
        }

        view?.reload()
    }

    override fun didUpdateCurrency(currency: Currency) {
        dataSource.currency = currency
        dataSource.clearRates()
        interactor.fetchRates(currency.code, dataSource.coinCodes)
        view?.reload()
    }

    override fun didUpdateBalance(coinCode: CoinCode, balance: Double) {
        val position = dataSource.getPosition(coinCode)
        dataSource.setBalance(position, balance)
        view?.updateItem(position)
        view?.updateHeader()
    }

    override fun didUpdateState(coinCode: String, state: AdapterState) {
        val position = dataSource.getPosition(coinCode)
        dataSource.setState(position, state)
        view?.updateItem(position)
        view?.updateHeader()
    }

    override fun didUpdateRate(rate: Rate) {
        val position = dataSource.getPosition(rate.coinCode)
        dataSource.setRate(position, rate)
        view?.updateItem(position)
        view?.updateHeader()
    }

    //    override fun didUpdate() {
////        updateView()
//    }

    override fun didRefresh() {
        view?.didRefresh()
    }

    override fun openManageCoins() {
        router.openManageCoins()
    }

//    private fun updateView() {
//        val wallets = interactor.wallets
//        val rateObservables = wallets.map { interactor.rate(it.coinCode) }
//
//        Maybe.merge(rateObservables)
//                .toList()
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe { rates ->
//
//                    var totalBalance = 0.0
//                    val viewItems = mutableListOf<BalanceViewItem>()
//                    val currency = interactor.baseCurrency
//                    var allSynced = true
//
//                    wallets.forEach { wallet ->
//                        val rate = rates.firstOrNull { it.coinCode == wallet.coinCode }
//                        val balance = wallet.adapter.balance
//
//                        val rateExpired = rate?.expired ?: true
//
//                        rate?.let { mRate ->
//                            totalBalance += balance * mRate.value
//                        }
//
//                        viewItems.add(BalanceViewItem(
//                                coinValue = CoinValue(coinCode = wallet.coinCode, value = balance),
//                                exchangeValue = rate?.let { CurrencyValue(currency = currency, value = it.value) },
//                                currencyValue = rate?.let { CurrencyValue(currency = currency, value = balance * it.value) },
//                                state = wallet.adapter.state,
//                                rateExpired = rateExpired
//                        ))
//
//                        if (wallet.adapter.state !is AdapterState.Synced) {
//                            allSynced = false
//                        }
//
//                        if (balance > 0) {
//                            allSynced = allSynced && rate != null && !rateExpired
//                        }
//                    }
//
//                    view?.updateBalanceColor(if (allSynced) R.color.yellow_crypto else R.color.yellow_crypto_40)
//                    view?.show(totalBalance = CurrencyValue(currency = currency, value = totalBalance))
//                    view?.show(wallets = viewItems)
//
//                }.let {
//                    disposables.add(it)
//                }
//    }

}
