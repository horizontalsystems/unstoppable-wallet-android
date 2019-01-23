package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.TransactionRecord

class TransactionsLoader(private val dataSource: TransactionRecordDataSource) {

    interface Delegate {
        fun didChangeData()
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
            if (dataSource.increasePage()) {
                delegate?.didChangeData()
            }
            loading = false
        } else {
            delegate?.fetchRecords(fetchDataList)
        }
    }

    fun didFetchRecords(records: Map<CoinCode, List<TransactionRecord>>) {
        dataSource.handleNextRecords(records)
        if (dataSource.increasePage()) {
            delegate?.didChangeData()
        }
        loading = false
    }

    fun itemIndexesForTimestamp(coinCode: CoinCode, timestamp: Long): List<Int> {
        return dataSource.itemIndexesForTimestamp(coinCode, timestamp)
    }

    fun didUpdateRecords(records: List<TransactionRecord>, coinCode: CoinCode) {
        if (dataSource.handleUpdatedRecords(records, coinCode)) {
            delegate?.didChangeData()
        }
    }

}
