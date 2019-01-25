package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.TransactionRecord

class TransactionsPresenter(private val interactor: TransactionsModule.IInteractor,
                            private val router: TransactionsModule.IRouter,
                            private val factory: TransactionViewItemFactory,
                            private val loader: TransactionsLoader,
                            private val metadataDataSource: TransactionMetadataDataSource) : TransactionsModule.IViewDelegate, TransactionsModule.IInteractorDelegate, TransactionsLoader.Delegate {

    var view: TransactionsModule.IView? = null

    override fun viewDidLoad() {
        interactor.initialFetch()
    }

    override fun onTransactionItemClick(transaction: TransactionViewItem) {
        router.openTransactionInfo(transaction)
    }

    override fun onFilterSelect(coinCode: CoinCode?) {
        interactor.setSelectedCoinCodes(coinCode?.let { listOf(coinCode) } ?: listOf())
    }

    override fun onClear() {
        interactor.clear()
    }

    override val itemsCount: Int
        get() = loader.itemsCount

    override fun itemForIndex(index: Int): TransactionViewItem {
        val transactionItem = loader.itemForIndex(index)
        val coinCode = transactionItem.coinCode
        val lastBlockHeight = metadataDataSource.getLastBlockHeight(coinCode)
        val threshold = metadataDataSource.getConfirmationThreshold(coinCode)
        val rate = metadataDataSource.getRate(coinCode, transactionItem.record.timestamp)

        return factory.item(transactionItem, lastBlockHeight, threshold, rate)
    }

    override fun idForIndex(index: Int): Long {
        return loader.itemForIndex(index).record.transactionHash.hashCode().toLong()
    }

    override fun onBottomReached() {
        loader.loadNext(false)
    }

    override fun onUpdateCoinsData(allCoinData: List<Triple<String, Int, Int?>>) {
        val coinCodes = allCoinData.map { it.first }

        loader.setCoinCodes(coinCodes)

        allCoinData.forEach { (coinCode, confirmationThreshold, lastBlockHeight) ->
            metadataDataSource.setConfirmationThreshold(confirmationThreshold, coinCode)
            lastBlockHeight?.let {
                metadataDataSource.setLastBlockHeight(it, coinCode)
            }
        }


        loader.loadNext(true)

        view?.showFilters(listOf(null).plus(coinCodes))

        interactor.fetchLastBlockHeights()
    }

    override fun onUpdateSelectedCoinCodes(selectedCoinCodes: List<CoinCode>) {
        loader.setCoinCodes(selectedCoinCodes)
        loader.loadNext(true)
    }

    override fun didFetchRecords(records: Map<CoinCode, List<TransactionRecord>>) {
        loader.didFetchRecords(records)
        fetchRatesForRecords(records)
    }

    override fun onUpdateLastBlockHeight(coinCode: CoinCode, lastBlockHeight: Int) {
        metadataDataSource.setLastBlockHeight(lastBlockHeight, coinCode)
        view?.reload()
    }

    override fun onUpdateBaseCurrency() {
        metadataDataSource.clearRates()
        view?.reload()

        fetchRatesForRecords(loader.allRecords)
    }

    override fun didFetchRate(rateValue: Double, coinCode: CoinCode, currency: Currency, timestamp: Long) {
        metadataDataSource.setRate(rateValue, coinCode, currency, timestamp)

        val itemIndexes = loader.itemIndexesForTimestamp(coinCode, timestamp)
        if (itemIndexes.isNotEmpty()) {
            view?.reloadItems(itemIndexes)
        }
    }

    override fun didUpdateRecords(records: List<TransactionRecord>, coinCode: CoinCode) {
        loader.didUpdateRecords(records, coinCode)
        fetchRatesForRecords(mapOf(coinCode to records))
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

    private fun fetchRatesForRecords(records: Map<CoinCode, List<TransactionRecord>>) {
        interactor.fetchRates(records.map { Pair(it.key, it.value.map { it.timestamp }.distinct().sortedDescending()) }.toMap())
    }
}
