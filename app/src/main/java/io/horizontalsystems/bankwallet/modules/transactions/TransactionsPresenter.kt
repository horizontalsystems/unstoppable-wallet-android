package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class TransactionsPresenter(
        private val interactor: TransactionsModule.IInteractor,
        private val router: TransactionsModule.IRouter,
        private val dataSource: TransactionRecordDataSource)
    : TransactionsModule.IViewDelegate, TransactionsModule.IInteractorDelegate {

    var view: TransactionsModule.IView? = null
    var itemDetails: TransactionViewItem? = null

    private var adapterStates: MutableMap<Wallet, AdapterState> = mutableMapOf()

    override fun viewDidLoad() {
        interactor.initialFetch()
    }

    override fun onFilterSelect(wallet: Wallet?) {
        interactor.setSelectedWallets(wallet?.let { listOf(wallet) } ?: listOf())
    }

    override fun onClear() {
        interactor.clear()
    }

    override fun onBottomReached() {
        loadNext(false)
    }

    override fun willShow(transactionViewItem: TransactionViewItem) {
        if (transactionViewItem.currencyValue == null) {
            transactionViewItem.date?.let {
                interactor.fetchRate(transactionViewItem.wallet.coin, it.time / 1000)
            }
        }
    }

    override fun showDetails(item: TransactionViewItem) {
        itemDetails = item
    }

    override fun onUpdateWalletsData(allWalletsData: List<Pair<Wallet, LastBlockInfo?>>) {
        dataSource.onUpdateWalletsData(allWalletsData)

        interactor.fetchLastBlockHeights()

        val wallets = allWalletsData.map { it.first }
        val filters = when {
            wallets.size < 2 -> listOf()
            else -> listOf(null).plus(getOrderedList(wallets))
        }

        view?.showFilters(filters)

        loadNext(true)
    }

    override fun onUpdateSelectedWallets(selectedWallets: List<Wallet>) {
        dataSource.setWallets(selectedWallets)
        loadNext(true)
    }

    override fun didFetchRecords(records: Map<Wallet, List<TransactionRecord>>, initial: Boolean) {
        dataSource.handleNextRecords(records)
        if (dataSource.increasePage()) {
            view?.showTransactions(dataSource.itemsCopy)
        } else if (initial) {
            view?.showNoTransactions()
        }
        loading = false
    }

    override fun onUpdateLastBlock(wallet: Wallet, lastBlockInfo: LastBlockInfo) {
        if (dataSource.setLastBlock(wallet, lastBlockInfo)) {
            view?.showTransactions(dataSource.itemsCopy)
        }
    }

    override fun onUpdateBaseCurrency() {
        dataSource.clearRates()
        view?.showTransactions(dataSource.itemsCopy)
    }

    override fun didFetchRate(rateValue: BigDecimal, coin: Coin, currency: Currency, timestamp: Long) {
        if (dataSource.setRate(rateValue, coin, currency, timestamp)) {
            view?.showTransactions(dataSource.itemsCopy)
        }
    }

    override fun didUpdateRecords(records: List<TransactionRecord>, wallet: Wallet) {
        if (dataSource.handleUpdatedRecords(records, wallet)) {
            view?.showTransactions(dataSource.itemsCopy)
        }
    }

    override fun onConnectionRestore() {
        view?.reloadTransactions()
    }

    override fun initialAdapterStates(states: Map<Wallet, AdapterState>) {
        adapterStates = states.toMutableMap()
        syncState()
    }

    override fun onUpdateAdapterState(state: AdapterState, wallet: Wallet) {
        adapterStates[wallet] = state
        syncState()
    }

    private fun syncState() {
        if (adapterStates.any { it.value is AdapterState.Syncing }) {
            view?.showSyncing()
        } else {
            view?.hideSyncing()
        }
    }

    private fun getOrderedList(wallets: List<Wallet>): MutableList<Wallet> {
        val walletList = wallets.toMutableList()
        walletList.sortBy { it.coin.code }
        return walletList
    }

    private var loading: Boolean = false

    private fun loadNext(initial: Boolean = false) {
        if (loading) return
        loading = true

        if (dataSource.allShown) {
            if (initial) {
                view?.showNoTransactions()
            }
            loading = false
            return
        }

        interactor.fetchRecords(dataSource.getFetchDataList(), initial)
    }

}
