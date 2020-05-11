package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.math.BigDecimal

object BalanceModule {

    interface IView {
        fun set(viewItems: List<BalanceViewItem>)
        fun set(headerViewItem: BalanceHeaderViewItem)
        fun set(sortIsOn: Boolean)
        fun showBackupRequired(coin: Coin, predefinedAccountType: PredefinedAccountType)
        fun didRefresh()
        fun setBalanceHidden(hidden: Boolean, animate: Boolean)
        fun showSyncErrorDialog(coin: Coin)
    }

    interface IViewDelegate {
        fun onLoad()
        fun onRefresh()

        fun onReceive(viewItem: BalanceViewItem)
        fun onPay(viewItem: BalanceViewItem)
        fun onChart(viewItem: BalanceViewItem)
        fun onItem(viewItem: BalanceViewItem)

        fun onAddCoinClick()

        fun onSortTypeChange(sortType: BalanceSortType)
        fun onSortClick()
        fun onBackupClick()

        fun onClear()

        fun onResume()
        fun onPause()
        fun onHideBalanceClick()
        fun onShowBalanceClick()
        fun onSyncErrorClick(viewItem: BalanceViewItem)
    }

    interface IInteractor {
        val wallets: List<Wallet>
        val baseCurrency: Currency
        val sortType: BalanceSortType
        var balanceHidden: Boolean

        fun marketInfo(coinCode: String, currencyCode: String): MarketInfo?
        fun chartInfo(coinCode: String, currencyCode: String): ChartInfo?
        fun balance(wallet: Wallet): BigDecimal?
        fun balanceLocked(wallet: Wallet): BigDecimal?
        fun state(wallet: Wallet): AdapterState?

        fun subscribeToWallets()
        fun subscribeToBaseCurrency()
        fun subscribeToAdapters(wallets: List<Wallet>)

        fun subscribeToMarketInfo(currencyCode: String)

        fun refresh()
        fun predefinedAccountType(wallet: Wallet): PredefinedAccountType?

        fun saveSortType(sortType: BalanceSortType)

        fun clear()

        fun notifyPageActive()
        fun notifyPageInactive()
    }

    interface IInteractorDelegate {
        fun didUpdateWallets(wallets: List<Wallet>)
        fun didPrepareAdapters()
        fun didUpdateBalance(wallet: Wallet, balance: BigDecimal, balanceLocked: BigDecimal?)
        fun didUpdateState(wallet: Wallet, state: AdapterState)

        fun didUpdateCurrency(currency: Currency)
        fun didUpdateMarketInfo(marketInfo: Map<String, MarketInfo>)

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

    data class BalanceItem(val wallet: Wallet) {
        var balance: BigDecimal? = null
        var balanceLocked: BigDecimal? = null
        val balanceTotal: BigDecimal?
            get() {
                var result = balance ?: return null

                balanceLocked?.let { balanceLocked ->
                    result += balanceLocked
                }

                return result
            }

        var state: AdapterState? = null
        var marketInfo: MarketInfo? = null

        val fiatValue: BigDecimal?
            get() = marketInfo?.rate?.let { balance?.times(it) }
    }

    fun init(view: BalanceViewModel, router: IRouter) {
        val currencyManager = App.currencyManager

        val interactor = BalanceInteractor(
                App.walletManager,
                App.adapterManager, currencyManager,
                App.localStorage,
                App.xRateManager,
                App.predefinedAccountTypeManager,
                App.rateAppManager)

        val presenter = BalancePresenter(interactor, router, BalanceSorter(), App.predefinedAccountTypeManager,
                                         BalanceViewItemFactory())

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }
}
