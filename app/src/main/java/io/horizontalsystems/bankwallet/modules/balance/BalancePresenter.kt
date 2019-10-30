package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule.BalanceItem
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.math.BigDecimal

class BalancePresenter(
        private val interactor: BalanceModule.IInteractor,
        private val router: BalanceModule.IRouter,
        private val sorter: BalanceModule.IBalanceSorter,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager,
        private val factory: BalanceViewItemFactory,
        private val sortingOnThreshold: Int = 5
) : BalanceModule.IViewDelegate, BalanceModule.IInteractorDelegate {

    var view: BalanceModule.IView? = null

    private var accountToBackup: Account? = null
    private var items = listOf<BalanceItem>()
    private var currency: Currency = interactor.baseCurrency
    private var sortType: BalanceSortType = interactor.sortType

    private fun handleUpdate(wallets: List<Wallet>) {
        items = wallets.map {
            BalanceItem(it).apply {
                balance = interactor.balance(it)
                state = interactor.state(it)
            }
        }

        interactor.subscribeToAdapters(wallets)

        handleRates()
        handleStats()

        view?.set(sortIsOn = items.size >= sortingOnThreshold)

    }

    private fun handleRates() {
        items.forEach { item ->
            item.marketInfo = interactor.marketInfo(item.wallet.coin.code, currency.code)
        }
        interactor.subscribeToMarketInfo(currency.code)
    }

    private fun handleStats() {
        items.forEach { item ->
            item.chartInfo = interactor.chartInfo(item.wallet.coin.code, currency.code)
        }
        interactor.subscribeToChartInfo(items.map { it.wallet.coin.code }, currency.code)
    }

    private fun updateViewItems() {
        items = sorter.sort(items, sortType)
        val viewItems = items.map { item ->
            factory.viewItem(item, currency)
        }
        view?.set(viewItems = viewItems)
    }

    private fun updateHeaderViewItem() {
        val headerViewItem = factory.headerViewItem(items, currency)
        view?.set(headerViewItem = headerViewItem)
    }

    // IViewDelegate

    override fun viewDidLoad() {
        handleUpdate(wallets = interactor.wallets)

        interactor.subscribeToWallets()
        interactor.subscribeToBaseCurrency()

        handleStats()
        updateViewItems()
        updateHeaderViewItem()
    }

    override fun refresh() {
        interactor.refresh()
    }

    override fun onReceive(position: Int) {
        val wallet = items.getOrNull(position)?.wallet ?: return

        if (wallet.account.isBackedUp) {
            router.openReceive(wallet)
        } else {
            interactor.predefinedAccountType(wallet)?.let { predefinedAccountType ->
                accountToBackup = wallet.account
                view?.showBackupRequired(wallet.coin, predefinedAccountType)
            }
        }
    }

    override fun onPay(position: Int) {
        val wallet = items.getOrNull(position)?.wallet ?: return
        router.openSend(wallet)
    }

    override fun onChart(position: Int) {
        val wallet = items.getOrNull(position)?.wallet ?: return
        router.openChart(wallet.coin)
    }

    override fun openManageCoins() {
        router.openManageCoins()
    }

    override fun onSortClick() {
        router.openSortTypeDialog(sortType)
    }

    override fun onSortTypeChanged(sortType: BalanceSortType) {
        this.sortType = sortType
        interactor.saveSortType(sortType)

        if (sortType == BalanceSortType.PercentGrowth) {
            handleStats()
        }
        updateViewItems()
    }

    override fun onBackupClick() {
        accountToBackup?.let { account ->
            val accountType = predefinedAccountTypeManager.allTypes.first { it.supports(account.type) }
            router.openBackup(account, accountType.coinCodes)
            accountToBackup = null
        }
    }

    override fun onClear() {
        interactor.clear()
    }

    // IInteractorDelegate

    override fun didUpdateWallets(wallets: List<Wallet>) {
        handleUpdate(wallets)

        updateViewItems()
        updateHeaderViewItem()
    }

    override fun didUpdateBalance(wallet: Wallet, balance: BigDecimal) {
        val item = items.find { it.wallet == wallet } ?: return

        item.balance = balance

        updateViewItems()
        updateHeaderViewItem()
    }

    override fun didUpdateState(wallet: Wallet, state: AdapterState) {
        val item = items.find { it.wallet == wallet } ?: return

        item.state = state

        updateViewItems()
        updateHeaderViewItem()
    }

    override fun didUpdateCurrency(currency: Currency) {
        this.currency = currency
        handleRates()
        handleStats()

        updateViewItems()
        updateHeaderViewItem()
    }

    override fun didUpdateMarketInfo(marketInfo: Map<String, MarketInfo>) {
        items.forEach { item ->
            marketInfo[item.wallet.coin.code]?.let {
                item.marketInfo = it
            }
        }
        updateViewItems()
        updateHeaderViewItem()
    }

    override fun didUpdateChartInfo(chartInfo: ChartInfo, coinCode: String) {
        items.forEach { item ->
            if (item.wallet.coin.code == coinCode) {
                item.chartInfo = chartInfo
            }
        }
        updateViewItems()
    }

    override fun didRefresh() {
        view?.didRefresh()
    }

}
