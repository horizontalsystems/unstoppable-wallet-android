package io.horizontalsystems.bankwallet.modules.transactions

import android.support.v7.util.DiffUtil
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.TransactionRecord
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

    override fun onTransactionItemClick(transaction: TransactionViewItem) {
        router.openTransactionInfo(transaction)
    }

    override fun onFilterSelect(coin: Coin?) {
        interactor.setSelectedCoinCodes(coin?.let { listOf(coin) } ?: listOf())
    }

    override fun onClear() {
        interactor.clear()
    }

    override val itemsCount: Int
        get() = loader.itemsCount

    override fun itemForIndex(index: Int): TransactionViewItem {
        val transactionItem = loader.itemForIndex(index)
        val coin = transactionItem.coin
        val lastBlockHeight = metadataDataSource.getLastBlockHeight(coin)
        val threshold = metadataDataSource.getConfirmationThreshold(coin)
        val rate = metadataDataSource.getRate(coin, transactionItem.record.timestamp)

        return factory.item(transactionItem, lastBlockHeight, threshold, rate)
    }

    override fun onBottomReached() {
        loader.loadNext(false)
    }

    override fun onUpdateCoinsData(allCoinData: List<Triple<Coin, Int, Int?>>) {
        val coins = allCoinData.map { it.first }

        loader.setCoinCodes(coins)

        allCoinData.forEach { (coinCode, confirmationThreshold, lastBlockHeight) ->
            metadataDataSource.setConfirmationThreshold(confirmationThreshold, coinCode)
            lastBlockHeight?.let {
                metadataDataSource.setLastBlockHeight(it, coinCode)
            }
        }

        loader.loadNext(true)

        val filters = when {
            coins.size < 2 -> listOf()
            else -> listOf(null).plus(coins)
        }

        view?.showFilters(filters)

        interactor.fetchLastBlockHeights()
    }

    override fun onUpdateSelectedCoinCodes(selectedCoins: List<Coin>) {
        loader.setCoinCodes(selectedCoins)
        loader.loadNext(true)
    }

    override fun didFetchRecords(records: Map<Coin, List<TransactionRecord>>) {
        loader.didFetchRecords(records)
        fetchRatesForRecords(records)
    }

    override fun onUpdateLastBlockHeight(coin: Coin, lastBlockHeight: Int) {
        val oldBlockHeight = metadataDataSource.getLastBlockHeight(coin)
        val threshold = metadataDataSource.getConfirmationThreshold(coin)

        metadataDataSource.setLastBlockHeight(lastBlockHeight, coin)

        if (threshold == null || oldBlockHeight == null) {
            view?.reload()
            return
        }

        val indexes = loader.itemIndexesForPending(coin, oldBlockHeight - threshold)
        if (indexes.isNotEmpty()) {
            view?.reloadItems(indexes)
        }
    }

    override fun onUpdateBaseCurrency() {
        metadataDataSource.clearRates()
        view?.reload()

        fetchRatesForRecords(loader.allRecords)
    }

    override fun didFetchRate(rateValue: BigDecimal, coin: Coin, currency: Currency, timestamp: Long) {
        metadataDataSource.setRate(rateValue, coin, currency, timestamp)

        val itemIndexes = loader.itemIndexesForTimestamp(coin, timestamp)
        if (itemIndexes.isNotEmpty()) {
            view?.reloadItems(itemIndexes)
        }
    }

    override fun didUpdateRecords(records: List<TransactionRecord>, coin: Coin) {
        loader.didUpdateRecords(records, coin)

        fetchRatesForRecords(mapOf(coin to records))
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

    private fun fetchRatesForRecords(records: Map<Coin, List<TransactionRecord>>) {
        interactor.fetchRates(records.map { Pair(it.key, it.value.map { it.timestamp }.distinct().sortedDescending()) }.toMap())
    }
}
