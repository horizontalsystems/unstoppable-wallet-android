package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule.BalanceItem
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.math.BigDecimal
import java.util.concurrent.Executors

class BalancePresenter(
        private val interactor: BalanceModule.IInteractor,
        private val router: BalanceModule.IRouter,
        private val sorter: BalanceModule.IBalanceSorter,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager,
        private val factory: BalanceViewItemFactory,
        private val sortingOnThreshold: Int = 5
) : BalanceModule.IViewDelegate, BalanceModule.IInteractorDelegate {

    var view: BalanceModule.IView? = null

    private val executor = Executors.newSingleThreadExecutor()

    private var items = listOf<BalanceItem>()
    private var viewItems = mutableListOf<BalanceViewItem>()
    private val viewItemsCopy: List<BalanceViewItem>
        get() = viewItems.map { it.copy() }
    private var currency: Currency = interactor.baseCurrency
    private var sortType: BalanceSortType = interactor.sortType
    private var accountToBackup: Account? = null

    // IViewDelegate

    override fun onLoad() {
        executor.submit {
            interactor.subscribeToWallets()
            interactor.subscribeToBaseCurrency()

            handleUpdate(interactor.wallets)

            updateViewItems()
            updateHeaderViewItem()
        }
    }

    override fun onRefresh() {
        executor.submit {
            interactor.refresh()
        }
    }

    override fun onReceive(viewItem: BalanceViewItem) {
        val wallet = viewItem.wallet

        if (wallet.account.isBackedUp) {
            router.openReceive(wallet)
        } else {
            interactor.predefinedAccountType(wallet)?.let { predefinedAccountType ->
                accountToBackup = wallet.account
                view?.showBackupRequired(wallet.coin, predefinedAccountType)
            }
        }
    }

    override fun onPay(viewItem: BalanceViewItem) {
        router.openSend(viewItem.wallet)
    }

    override fun onChart(viewItem: BalanceViewItem) {
        router.openChart(viewItem.wallet.coin)
    }

    private var expandedViewItem: BalanceViewItem? = null

    override fun onItem(viewItem: BalanceViewItem) {
        val itemIndex = viewItems.indexOfFirst { it.wallet == viewItem.wallet }
        if (itemIndex == -1) return

        var indexToCollapse: Int = -1
        var indexToExpand: Int = -1

        if (viewItem.wallet == expandedViewItem?.wallet) {
            indexToCollapse = itemIndex

            expandedViewItem = null
        } else {
            expandedViewItem?.let { expandedViewItem ->
                indexToCollapse = viewItems.indexOfFirst { it.wallet == expandedViewItem.wallet }
            }

            indexToExpand = itemIndex

            expandedViewItem = viewItem
        }

        if (indexToCollapse != -1) {
            viewItems[indexToCollapse] = factory.viewItem(items[indexToCollapse], currency, BalanceViewItem.UpdateType.EXPANDED, false)
        }

        if (indexToExpand != -1) {
            viewItems[indexToExpand] = factory.viewItem(items[indexToExpand], currency, BalanceViewItem.UpdateType.EXPANDED, true)
        }

        view?.set(viewItemsCopy)
    }

    override fun onAddCoinClick() {
        router.openManageCoins()
    }

    override fun onSortClick() {
        router.openSortTypeDialog(sortType)
    }

    override fun onSortTypeChange(sortType: BalanceSortType) {
        executor.submit {
            this.sortType = sortType
            interactor.saveSortType(sortType)

            updateViewItems()
        }
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

    override fun onResume() {
        interactor.notifyPageActive()
    }

    override fun onPause() {
        interactor.notifyPageInactive()
    }

    // IInteractorDelegate

    override fun didUpdateWallets(wallets: List<Wallet>) {
        executor.submit {
            handleUpdate(wallets)

            updateViewItems()
            updateHeaderViewItem()
        }
    }

    override fun didPrepareAdapters() {
        executor.submit {
            handleAdaptersReady()

            updateViewItems()
            updateHeaderViewItem()
        }
    }

    override fun didUpdateBalance(wallet: Wallet, balance: BigDecimal, balanceLocked: BigDecimal?) {
        executor.submit {
            updateItem(wallet, { item ->
                item.balance = balance
                item.balanceLocked = balanceLocked
            }, BalanceViewItem.UpdateType.BALANCE)

            updateHeaderViewItem()
        }
    }

    override fun didUpdateState(wallet: Wallet, state: AdapterState) {
        executor.submit {
            updateItem(wallet, { item ->
                item.state = state
            }, BalanceViewItem.UpdateType.STATE)

            updateHeaderViewItem()
        }
    }

    override fun didUpdateCurrency(currency: Currency) {
        executor.submit {
            this.currency = currency

            handleRates()

            updateViewItems()
            updateHeaderViewItem()
        }
    }

    override fun didUpdateMarketInfo(marketInfo: Map<String, MarketInfo>) {
        executor.submit {
            items.forEachIndexed { index, item ->
                marketInfo[item.wallet.coin.code]?.let {
                    item.marketInfo = it
                    viewItems[index] = factory.viewItem(item, currency, BalanceViewItem.UpdateType.MARKET_INFO, viewItems[index].expanded)
                }
            }
            view?.set(viewItemsCopy)
            updateHeaderViewItem()
        }
    }

    override fun didRefresh() {
        view?.didRefresh()
    }

    private fun handleUpdate(wallets: List<Wallet>) {
        items = wallets.map { BalanceItem(it) }

        handleAdaptersReady()
        handleRates()

        view?.set(sortIsOn = items.size >= sortingOnThreshold)
    }

    private fun handleAdaptersReady() {
        interactor.subscribeToAdapters(items.map { it.wallet })

        items.forEach { item ->
            item.balance = interactor.balance(item.wallet)
            item.balanceLocked = interactor.balanceLocked(item.wallet)
            item.state = interactor.state(item.wallet)
        }
    }

    private fun handleRates() {
        interactor.subscribeToMarketInfo(currency.code)

        items.forEach { item ->
            item.marketInfo = interactor.marketInfo(item.wallet.coin.code, currency.code)
        }
    }

    private fun updateItem(wallet: Wallet, updateBlock: (BalanceItem) -> Unit, updateType: BalanceViewItem.UpdateType?) {
        val index = items.indexOfFirst { it.wallet == wallet }
        if (index == -1)
            return

        val item = items[index]
        updateBlock(item)
        viewItems[index] = factory.viewItem(item, currency, updateType, viewItems[index].expanded)

        view?.set(viewItemsCopy)
    }

    private fun updateViewItems() {
        items = sorter.sort(items, sortType)

        viewItems = items.map { factory.viewItem(it, currency, null, it.wallet == expandedViewItem?.wallet) }.toMutableList()

        view?.set(viewItemsCopy)
    }

    private fun updateHeaderViewItem() {
        val headerViewItem = factory.headerViewItem(items, currency)
        view?.set(headerViewItem)
    }

}
