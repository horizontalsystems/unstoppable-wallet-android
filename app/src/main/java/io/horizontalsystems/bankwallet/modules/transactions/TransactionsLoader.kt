package io.horizontalsystems.bankwallet.modules.transactions

import androidx.recyclerview.widget.DiffUtil
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.TransactionRecord

class TransactionsLoader(private val dataSource: TransactionRecordDataSource) {

    interface Delegate {
        fun onChange(diff: DiffUtil.DiffResult)
        fun didChangeData()
        fun didInsertData(fromIndex: Int, count: Int)
        fun fetchRecords(fetchDataList: List<TransactionsModule.FetchData>)
    }

    var delegate: Delegate? = null

    val itemsCount: Int
        get() = dataSource.itemsCount

    var loading: Boolean = false
    val allRecords: Map<Coin, List<TransactionRecord>>
        get() = dataSource.allRecords

    fun itemForIndex(index: Int) =
            dataSource.itemForIndex(index)

    fun setCoinCodes(coins: List<Coin>) {
        dataSource.setCoinCodes(coins)
    }

    fun handleUpdate(coins: List<Coin>) {
        dataSource.handleUpdatedCoins(coins)
    }

    fun loadNext(initial: Boolean = false) {
        if (loading) return
        loading = true

        if (dataSource.allShown) {
            if (initial) {
                delegate?.didChangeData()
            }
            loading = false
            return
        }

        val fetchDataList = dataSource.getFetchDataList()
        if (fetchDataList.isEmpty()) {
            val currentItemsCount = dataSource.itemsCount
            val insertedCount = dataSource.increasePage()
            if (insertedCount > 0) {
                delegate?.didInsertData(currentItemsCount, insertedCount)
            }
            loading = false
        } else {
            delegate?.fetchRecords(fetchDataList)
        }
    }

    fun didFetchRecords(records: Map<Coin, List<TransactionRecord>>) {
        dataSource.handleNextRecords(records)
        val currentItemsCount = dataSource.itemsCount
        val insertedCount = dataSource.increasePage()
        if (insertedCount > 0) {
            delegate?.didInsertData(currentItemsCount, insertedCount)
        }
        loading = false
    }

    fun itemIndexesForTimestamp(coin: Coin, timestamp: Long): List<Int> {
        return dataSource.itemIndexesForTimestamp(coin, timestamp)
    }

    fun itemIndexesForPending(coin: Coin, thresholdBlockHeight: Int): List<Int> {
        return dataSource.itemIndexesForPending(coin, thresholdBlockHeight)
    }

    fun didUpdateRecords(records: List<TransactionRecord>, coin: Coin) {
        dataSource.handleUpdatedRecords(records, coin)?.let {
            delegate?.onChange(it)
        }
    }

}
