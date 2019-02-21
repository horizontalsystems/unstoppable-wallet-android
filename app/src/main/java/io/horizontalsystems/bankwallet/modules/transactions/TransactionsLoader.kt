package io.horizontalsystems.bankwallet.modules.transactions

import android.support.v7.util.DiffUtil
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
    val allRecords: Map<CoinCode, List<TransactionRecord>>
        get() = dataSource.allRecords

    fun itemForIndex(index: Int) =
            dataSource.itemForIndex(index)

    fun setCoinCodes(coinCodes: List<CoinCode>) {
        dataSource.setCoinCodes(coinCodes)
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

    fun didFetchRecords(records: Map<CoinCode, List<TransactionRecord>>) {
        dataSource.handleNextRecords(records)
        val currentItemsCount = dataSource.itemsCount
        val insertedCount = dataSource.increasePage()
        if (insertedCount > 0) {
            delegate?.didInsertData(currentItemsCount, insertedCount)
        }
        loading = false
    }

    fun itemIndexesForTimestamp(coinCode: CoinCode, timestamp: Long): List<Int> {
        return dataSource.itemIndexesForTimestamp(coinCode, timestamp)
    }

    fun itemIndexesForPending(coinCode: CoinCode, thresholdBlockHeight: Int): List<Int> {
        return dataSource.itemIndexesForPending(coinCode, thresholdBlockHeight)
    }

    fun didUpdateRecords(records: List<TransactionRecord>, coinCode: CoinCode) {
        dataSource.handleUpdatedRecords(records, coinCode)?.let {
            delegate?.onChange(it)
        }
    }

}
