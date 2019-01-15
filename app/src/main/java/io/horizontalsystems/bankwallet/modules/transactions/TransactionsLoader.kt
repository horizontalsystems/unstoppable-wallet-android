package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.TransactionRecord

class TransactionsLoader(private val dataSource: TransactionRecordDataSource) {

    interface Delegate {
        fun didChangeData()
        fun fetchRecords(fetchDataList: List<TransactionsModule.FetchData>)
    }

    var delegate : Delegate? = null

    val itemsCount: Int
        get() = dataSource.itemsCount

    var loading: Boolean = false

    fun itemForIndex(index: Int) =
            dataSource.itemForIndex(index)

    fun setCoinCodes(coinCodes: List<CoinCode>) {
        dataSource.setCoinCodes(coinCodes)
    }

    fun loadNext() {
        if (dataSource.allShown) return

        val fetchDataList = dataSource.getFetchDataList()
        if (fetchDataList.isEmpty()) {
            dataSource.increasePage()
            delegate?.didChangeData()
        } else {
            delegate?.fetchRecords(fetchDataList)
        }
    }

    fun didFetchRecords(records: Map<CoinCode, List<TransactionRecord>>) {
        dataSource.handleNextRecords(records)
        dataSource.increasePage()
        delegate?.didChangeData()
    }

}
