package io.horizontalsystems.bankwallet.modules.transactions

import androidx.recyclerview.widget.DiffUtil
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class TransactionsPresenter(
        private val interactor: TransactionsModule.IInteractor,
        private val router: TransactionsModule.IRouter,
        private val factory: TransactionViewItemFactory,
        private val loader: TransactionsLoader,
        private val metadataDataSource: TransactionMetadataDataSource)
    : TransactionsModule.IViewDelegate, TransactionsModule.IInteractorDelegate, TransactionsLoader.Delegate {

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
        get() = loader.itemsCount

    override fun itemForIndex(index: Int): TransactionViewItem {
        val transactionItem = loader.itemForIndex(index)
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
        loader.loadNext(false)
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

        loader.handleUpdate(wallets)
        loader.loadNext(true)
    }

    override fun onUpdateSelectedWallets(selectedWallets: List<Wallet>) {
        loader.setWallets(selectedWallets)
        loader.loadNext(true)
    }

    override fun didFetchRecords(records: Map<Wallet, List<TransactionRecord>>) {
        loader.didFetchRecords(records)
    }

    override fun onUpdateLastBlock(wallet: Wallet, lastBlockInfo: LastBlockInfo) {
        val oldBlockInfo = metadataDataSource.getLastBlockInfo(wallet)
        val threshold = metadataDataSource.getConfirmationThreshold(wallet)

        metadataDataSource.setLastBlockInfo(lastBlockInfo, wallet)

        if (oldBlockInfo == null) {
            view?.reload()
            return
        }

        val indexes = loader.itemIndexesForPending(wallet, oldBlockInfo.height - threshold).toMutableList()
        lastBlockInfo.timestamp?.let { lastBlockTimestamp ->
            val lockedIndexes = loader.itemIndexesForLocked(wallet, lastBlockTimestamp, oldBlockInfo.timestamp)
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

        val itemIndexes = loader.itemIndexesForTimestamp(coin, timestamp)
        if (itemIndexes.isNotEmpty()) {
            view?.reloadItems(itemIndexes)
        }
    }

    override fun didUpdateRecords(records: List<TransactionRecord>, wallet: Wallet) {
        loader.didUpdateRecords(records, wallet)
    }

    override fun onConnectionRestore() {
        view?.reload()
    }

    //
    // TransactionsLoader Delegate
    //

    override fun onChange(diff: DiffUtil.DiffResult) {
        view?.reloadChange(diff)
    }

    override fun didChangeData() {
        view?.reload()
    }

    override fun didInsertData(fromIndex: Int, count: Int) {
        view?.addItems(fromIndex, count)
    }

    override fun fetchRecords(fetchDataList: List<TransactionsModule.FetchData>) {
        interactor.fetchRecords(fetchDataList)
    }

    private fun getOrderedList(wallets: List<Wallet>): MutableList<Wallet> {
        val walletList = wallets.toMutableList()
        walletList.sortBy { it.coin.code }
        return walletList
    }

}
