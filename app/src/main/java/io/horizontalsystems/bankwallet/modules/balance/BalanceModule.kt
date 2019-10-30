package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.math.BigDecimal

object BalanceModule {

    interface IView {
        fun set(viewItems: List<BalanceViewItem>)
        fun set(headerViewItem: BalanceHeaderViewItem)
        fun set(statsButtonState: StatsButtonState)
        fun set(sortIsOn: Boolean)
        fun showBackupRequired(coin: Coin, predefinedAccountType: IPredefinedAccountType)
        fun didRefresh()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun refresh()

        fun onReceive(position: Int)
        fun onPay(position: Int)
        fun onChart(position: Int)

        fun openManageCoins()

        fun onSortTypeChanged(sortType: BalanceSortType)
        fun onSortClick()
        fun onBackupClick()
        fun onStatsSwitch()

        fun onClear()
    }

    interface IInteractor {
        val wallets: List<Wallet>
        val baseCurrency: Currency
        val sortType: BalanceSortType

        fun marketInfo(coinCode: String, currencyCode: String): MarketInfo?
        fun chartInfo(coinCode: String, currencyCode: String): ChartInfo?
        fun balance(wallet: Wallet): BigDecimal?
        fun state(wallet: Wallet): AdapterState?

        fun subscribeToWallets()
        fun subscribeToBaseCurrency()
        fun subscribeToAdapters(wallets: List<Wallet>)

        fun subscribeToMarketInfo(currencyCode: String)
        fun subscribeToChartInfo(coinCodes: List<String>, currencyCode: String)
        fun unsubscribeFromChartInfo()

        fun refresh()
        fun predefinedAccountType(wallet: Wallet): IPredefinedAccountType?

        fun saveSortType(sortType: BalanceSortType)

        fun clear()
    }

    interface IInteractorDelegate {
        fun didUpdateWallets(wallets: List<Wallet>)
        fun didUpdateBalance(wallet: Wallet, balance: BigDecimal)
        fun didUpdateState(wallet: Wallet, state: AdapterState)

        fun didUpdateCurrency(currency: Currency)
        fun didUpdateMarketInfo(marketInfo: Map<String, MarketInfo>)
        fun didUpdateChartInfo(chartInfo: ChartInfo, coinCode: String)

        fun didRefresh()
    }

    interface IRouter {
        fun openReceive(wallet: Wallet)
        fun openSend(wallet: Wallet)
        fun openManageCoins()
        fun openSortTypeDialog(sortingType: BalanceSortType)
        fun openBackup(account: Account, coinCodesStringRes: Int)
        fun openChart(coin: Coin)
    }

    interface IBalanceSorter {
        fun sort(items: List<BalanceItem>, sortType: BalanceSortType): List<BalanceItem>
    }

    enum class StatsButtonState {
        NORMAL, HIDDEN, SELECTED
    }

    data class BalanceItem(val wallet: Wallet) {

        var balance: BigDecimal? = null
        var state: AdapterState? = null
        var marketInfo: MarketInfo? = null
        var chartInfo: ChartInfo? = null

        val fiatValue: BigDecimal?
            get() = marketInfo?.rate?.let { balance?.times(it) }
    }

    fun init(view: BalanceViewModel, router: IRouter) {
        val currencyManager = App.currencyManager
        val interactor = BalanceInteractor(App.walletManager, App.adapterManager, currencyManager, App.localStorage, App.xRateManager, App.predefinedAccountTypeManager)
        val presenter = BalancePresenter(interactor, router, BalanceSorter(), App.predefinedAccountTypeManager, BalanceViewItemFactory())

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }
}
