package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class TransactionsPresenter(
        private val interactor: TransactionsModule.IInteractor,
        private val router: TransactionsModule.IRouter,
        private val factory: TransactionViewItemFactory,
        private val dataSource: TransactionRecordDataSource,
        private val metadataDataSource: TransactionMetadataDataSource)
    : TransactionsModule.IViewDelegate, TransactionsModule.IInteractorDelegate {

    var view: TransactionsModule.IView? = null

    override fun viewDidLoad() {
        interactor.initialFetch()
    }

    override fun onVisible() {
        view?.reload()
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

    override val itemsCount: Int
        get() = dataSource.itemsCount

    override fun itemForIndex(index: Int): TransactionViewItem {
        val transactionItem = dataSource.itemForIndex(index)
        val wallet = transactionItem.wallet
        val lastBlockInfo = metadataDataSource.getLastBlockInfo(wallet)
        val threshold = metadataDataSource.getConfirmationThreshold(wallet)
        val rate = metadataDataSource.getRate(wallet.coin, transactionItem.record.timestamp)

        if (rate == null) {
            interactor.fetchRate(wallet.coin, transactionItem.record.timestamp)
        }

        return factory.item(wallet, transactionItem, lastBlockInfo, threshold, rate)
    }

    override fun onBottomReached() {
        loadNext(false)
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

    override fun didFetchRecords(records: Map<Wallet, List<TransactionRecord>>) {
        dataSource.handleNextRecords(records)
        val currentItemsCount = dataSource.itemsCount
        val insertedCount = dataSource.increasePage()
        if (insertedCount > 0) {
            view?.addItems(currentItemsCount, insertedCount)
        }
        loading = false
    }

    override fun onUpdateLastBlock(wallet: Wallet, lastBlockInfo: LastBlockInfo) {
        val oldBlockInfo = metadataDataSource.getLastBlockInfo(wallet)
        val threshold = metadataDataSource.getConfirmationThreshold(wallet)

        metadataDataSource.setLastBlockInfo(lastBlockInfo, wallet)

        if (oldBlockInfo == null) {
            view?.reload()
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
        view?.reload()
    }

    override fun didFetchRate(rateValue: BigDecimal, coin: Coin, currency: Currency, timestamp: Long) {
        metadataDataSource.setRate(rateValue, coin, currency, timestamp)

        val itemIndexes = dataSource.itemIndexesForTimestamp(coin, timestamp)
        if (itemIndexes.isNotEmpty()) {
            view?.reloadItems(itemIndexes)
        }
    }

    override fun didUpdateRecords(records: List<TransactionRecord>, wallet: Wallet) {
        dataSource.handleUpdatedRecords(records, wallet)?.let {
            view?.reloadChange(it)
        }
    }

    override fun onConnectionRestore() {
        view?.reload()
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
                view?.reload()
            }
            loading = false
            return
        }

        interactor.fetchRecords(dataSource.getFetchDataList())
    }

}
