package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule.BalanceItem
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.xrateskit.entities.LatestRate
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
    private var hideBalance = interactor.balanceHidden

    // IViewDelegate

    override fun onLoad() {
        executor.submit {
            view?.setBalanceHidden(hideBalance, false)

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

    override fun onSwap(viewItem: BalanceViewItem) {
        router.openSwap(viewItem.wallet)
    }

    override fun onChart(viewItem: BalanceViewItem) {
        val balanceItem = items.firstOrNull { it.wallet == viewItem.wallet }
        if (balanceItem?.latestRate != null) {
            router.openChart(viewItem.wallet.coin)
        }
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
            viewItems[indexToCollapse] = factory.viewItem(items[indexToCollapse], currency, false, hideBalance)
        }

        if (indexToExpand != -1) {
            viewItems[indexToExpand] = factory.viewItem(items[indexToExpand], currency, true, hideBalance)
        }

        view?.set(viewItemsCopy)
    }

    override fun onAddCoinClick() {
        router.openManageCoins()
    }

    override fun onSortClick() {
        router.openSortTypeDialog(sortType)
    }

    override fun onHideBalanceClick() {
        setBalanceHidden(hidden = true)
    }

    override fun onShowBalanceClick() {
        setBalanceHidden(hidden = false)
    }

    private fun setBalanceHidden(hidden: Boolean) {
        interactor.balanceHidden = hidden
        hideBalance = hidden
        view?.setBalanceHidden(hidden, true)
        toggleBalanceVisibility()
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

    override fun onSyncErrorClick(viewItem: BalanceViewItem) {
        val state = items.firstOrNull { it.wallet == viewItem.wallet }?.state ?: return
        val nonSyncedState = (state as? AdapterState.NotSynced) ?: return
        val errorMessage = nonSyncedState.error.message ?: ""

        if (interactor.networkAvailable){
            view?.showSyncErrorDialog(viewItem.wallet, errorMessage, sourceChangeable(viewItem.wallet.coin.type))
        } else {
            view?.showNetworkNotAvailable()
        }
    }

    override fun onReportClick(errorMessage: String) {
        router.openEmail(interactor.reportEmail, errorMessage)
    }

    override fun refreshByWallet(wallet: Wallet) {
        interactor.refreshByWallet(wallet)
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
            updateItem(wallet) { item ->
                item.balance = balance
                item.balanceLocked = balanceLocked
            }

            updateHeaderViewItem()
        }
    }

    override fun didUpdateState(wallet: Wallet, state: AdapterState) {
        executor.submit {
            updateItem(wallet) { item ->
                item.state = state
            }

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

    override fun didUpdateLatestRate(latestRate: Map<CoinType, LatestRate>) {
        executor.submit {
            items.forEachIndexed { index, item ->
                latestRate[item.wallet.coin.type]?.let {
                    item.latestRate = it
                    viewItems[index] = factory.viewItem(item, currency, viewItems[index].expanded, hideBalance)
                }
            }
            view?.set(viewItemsCopy)
            updateHeaderViewItem()
        }
    }

    override fun didRefresh() {
        view?.didRefresh()
    }

    private fun sourceChangeable(coinType: CoinType): Boolean {
        return when(coinType) {
            is CoinType.Bep2,
            is CoinType.Ethereum,
            is CoinType.Erc20 -> false
            else -> true
        }
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
            item.latestRate = interactor.latestRate(item.wallet.coin.type, currency.code)
        }
    }

    private fun updateItem(wallet: Wallet, updateBlock: (BalanceItem) -> Unit) {
        val index = items.indexOfFirst { it.wallet == wallet }
        if (index == -1)
            return

        val item = items[index]
        updateBlock(item)
        viewItems[index] = factory.viewItem(item, currency, viewItems[index].expanded, hideBalance)

        view?.set(viewItemsCopy)
    }

    private fun toggleBalanceVisibility() {
        viewItems.forEach {
            it.hideBalance = hideBalance
        }
        view?.set(viewItemsCopy)
    }

    private fun updateViewItems() {
        items = sorter.sort(items, sortType)

        viewItems = items.map { factory.viewItem(it, currency, it.wallet == expandedViewItem?.wallet, hideBalance) }.toMutableList()

        view?.set(viewItemsCopy)
    }

    private fun updateHeaderViewItem() {
        val headerViewItem = factory.headerViewItem(items, currency)
        view?.set(headerViewItem)
    }

}
