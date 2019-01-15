package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.TransactionItem
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule.FetchData

class TransactionRecordDataSource {

    val itemsCount
        get() = items.size

    val allShown
        get() = coinCodes.all { pools[it]!!.allShown }

    private var pools = mutableMapOf<CoinCode, Pool>()
    private val items = mutableListOf<TransactionItem>()
    private var coinCodes = listOf<CoinCode>()
    private val limit = 10

    fun getFetchDataList() = coinCodes.mapNotNull { coinCode ->
        pools[coinCode]?.getFetchData(coinCode, limit)
    }

    fun handleNextRecords(records: Map<CoinCode, List<TransactionRecord>>) {
        records.forEach { (coinCode, transactionRecords) ->
            pools[coinCode]?.add(transactionRecords)
        }
        increasePage()
    }

    fun increasePage() {
        val unusedItems = mutableListOf<TransactionItem>()

        coinCodes.forEach { coinCode ->
            pools[coinCode]?.unusedRecords()?.forEach {
                unusedItems.add(TransactionItem(coinCode, it))
            }
        }

        unusedItems.sortByDescending { it.record.timestamp }

        val usedItems = unusedItems.take(limit)

        items.addAll(usedItems)

        usedItems.forEach {
            pools[it.coinCode]?.increaseFirstUnusedIndex()
        }
    }

    fun itemForIndex(index: Int) = items[index]

    fun setCoinCodes(coinCodes: List<CoinCode>) {
        pools.values.forEach {
            it.resetLastUnusedIndex()
        }

        coinCodes.forEach {
            if (!pools.containsKey(it)) {
                pools[it] = Pool()
            }
        }

        this.coinCodes = coinCodes
        items.clear()
    }
}

class Pool {

    private val records = mutableListOf<TransactionRecord>()
    private var firstUnusedIndex = 0
    var allLoaded = false
        private set

    val allShown: Boolean
        get() = allLoaded && unusedRecords().isEmpty()

    fun unusedRecords(): List<TransactionRecord> {
        return when {
            records.isEmpty() -> listOf()
            else -> records.subList(firstUnusedIndex, records.size)
        }
    }

    fun increaseFirstUnusedIndex() {
        firstUnusedIndex++
    }

    fun resetLastUnusedIndex() {
        firstUnusedIndex = 0
    }

    fun getFetchData(coinCode: CoinCode, limit: Int): FetchData? {
        if (allLoaded) {
            return null
        }

        val unusedRecordsSize = unusedRecords().size
        if (unusedRecordsSize > limit) {
            return null
        }

        val hashFrom = records.lastOrNull()?.transactionHash
        val fetchLimit = limit + 1 - unusedRecordsSize

        return FetchData(coinCode, hashFrom, fetchLimit)

    }

    fun add(transactionRecords: List<TransactionRecord>) {
        if (transactionRecords.isEmpty()) {
            allLoaded = true
        } else {
            records.addAll(transactionRecords)
        }
    }

}
