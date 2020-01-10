package io.horizontalsystems.bankwallet.modules.transactions

import androidx.recyclerview.widget.DiffUtil
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet

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
    val allRecords: Map<Wallet, List<TransactionRecord>>
        get() = dataSource.allRecords

    fun itemForIndex(index: Int) =
            dataSource.itemForIndex(index)

    fun setWallets(coins: List<Wallet>) {
        dataSource.setWallets(coins)
    }

    fun handleUpdate(wallets: List<Wallet>) {
        dataSource.handleUpdatedWallets(wallets)
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

    fun didFetchRecords(records: Map<Wallet, List<TransactionRecord>>) {
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

    fun itemIndexesForPending(wallet: Wallet, thresholdBlockHeight: Int): List<Int> {
        return dataSource.itemIndexesForPending(wallet, thresholdBlockHeight)
    }

    fun didUpdateRecords(records: List<TransactionRecord>, wallet: Wallet) {
        dataSource.handleUpdatedRecords(records, wallet)?.let {
            delegate?.onChange(it)
        }
    }

    fun itemIndexesForLocked(wallet: Wallet, unlockingBefore: Long, oldBlockTimestamp: Long?): List<Int> {
        return dataSource.itemIndexesForLocked(wallet, unlockingBefore, oldBlockTimestamp)
    }

}
