package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class TransactionsPresenter(
        private val interactor: TransactionsModule.IInteractor,
        private val router: TransactionsModule.IRouter,
        private val dataSource: TransactionRecordDataSource,
        private val metadataDataSource: TransactionMetadataDataSource)
    : TransactionsModule.IViewDelegate, TransactionsModule.IInteractorDelegate {

    var view: TransactionsModule.IView? = null

    override fun viewDidLoad() {
        interactor.initialFetch()
    }

    override fun onVisible() {
        view?.showTransactions(dataSource.items)
    }

    override fun onTransactionItemClick(transaction: TransactionViewItem) {
        router.openTransactionInfo(transaction)
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
        if (transactionViewItem.rate == null) {
            transactionViewItem.date?.let {
                interactor.fetchRate(transactionViewItem.wallet.coin, it.time / 1000)
            }
        }
    }

    override fun onUpdateWalletsData(allWalletsData: List<Triple<Wallet, Int, LastBlockInfo?>>) {
        val wallets = allWalletsData.map { it.first }

        allWalletsData.forEach { (wallet, confirmationThreshold, lastBlockHeight) ->
            metadataDataSource.setConfirmationThreshold(confirmationThreshold, wallet)
            lastBlockHeight?.let {
                metadataDataSource.setLastBlockInfo(it, wallet)
            }
        }

        interactor.fetchLastBlockHeights()

        val filters = when {
            wallets.size < 2 -> listOf()
            else -> listOf(null).plus(getOrderedList(wallets))
        }

        view?.showFilters(filters)

        dataSource.handleUpdatedWallets(wallets)
        loadNext(true)
    }

    override fun onUpdateSelectedWallets(selectedWallets: List<Wallet>) {
        dataSource.setWallets(selectedWallets)
        loadNext(true)
    }

    override fun didFetchRecords(records: Map<Wallet, List<TransactionRecord>>, initial: Boolean) {
        dataSource.handleNextRecords(records)
        val insertedItems = dataSource.increasePage()
        if (insertedItems.isNotEmpty()) {
            if (initial) {
                view?.showTransactions(dataSource.items)
            } else {
                view?.addTransactions(insertedItems)
            }
        } else if (initial) {
            view?.showNoTransactions()
        }
        loading = false
    }

    override fun onUpdateLastBlock(wallet: Wallet, lastBlockInfo: LastBlockInfo) {
        val oldBlockInfo = metadataDataSource.getLastBlockInfo(wallet)
        val threshold = metadataDataSource.getConfirmationThreshold(wallet)

        metadataDataSource.setLastBlockInfo(lastBlockInfo, wallet)

        if (oldBlockInfo == null) {
            view?.showTransactions(dataSource.items)
            return
        }

        val indexes = dataSource.itemIndexesForPending(wallet, oldBlockInfo.height - threshold).toMutableList()
        lastBlockInfo.timestamp?.let { lastBlockTimestamp ->
            val lockedIndexes = dataSource.itemIndexesForLocked(wallet, lastBlockTimestamp, oldBlockInfo.timestamp)
            indexes.addAll(lockedIndexes)
        }

        if (indexes.isNotEmpty()) {
            view?.reloadItems(indexes)
        }
    }

    override fun onUpdateBaseCurrency() {
        metadataDataSource.clearRates()
        view?.showTransactions(dataSource.items)
    }

    override fun didFetchRate(rateValue: BigDecimal, coin: Coin, currency: Currency, timestamp: Long) {
        if (dataSource.setRate(rateValue, coin, currency, timestamp)) {
            view?.showTransactions(dataSource.items)
        }
    }

    override fun didUpdateRecords(records: List<TransactionRecord>, wallet: Wallet) {
        if (dataSource.handleUpdatedRecords(records, wallet)) {
            view?.showTransactions(dataSource.items)
        }
    }

    override fun onConnectionRestore() {
        view?.showTransactions(dataSource.items)
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
