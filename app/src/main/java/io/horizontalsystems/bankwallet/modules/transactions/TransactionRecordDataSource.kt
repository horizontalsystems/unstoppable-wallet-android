package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.TransactionItem
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule.FetchData

class TransactionRecordDataSource(
        private val poolRepo: PoolRepo,
        private val itemsDataSource: TransactionItemDataSource,
        private val factory: TransactionItemFactory,
        private val limit: Int = 10) {

    val itemsCount
        get() = itemsDataSource.count

    val allShown
        get() = poolRepo.activePools.all { it.allShown }

    val allRecords: Map<CoinCode, List<TransactionRecord>>
        get() = poolRepo.activePools.map {
            Pair(it.coinCode, it.records)
        }.toMap()

    fun itemForIndex(index: Int): TransactionItem =
            itemsDataSource.itemForIndex(index)

    fun itemIndexesForTimestamp(coinCode: CoinCode, timestamp: Long): List<Int> =
            itemsDataSource.itemIndexesForTimestamp(coinCode, timestamp)

    fun getFetchDataList(): List<FetchData> = poolRepo.activePools.mapNotNull {
        it.getFetchData(limit)
    }

    fun handleNextRecords(records: Map<CoinCode, List<TransactionRecord>>) {
        records.forEach { (coinCode, transactionRecords) ->
            poolRepo.getPool(coinCode)?.add(transactionRecords)
        }
    }

    fun handleUpdatedRecords(records: List<TransactionRecord>, coinCode: CoinCode): Boolean {
        val pool = poolRepo.getPool(coinCode) ?: return false

        val (updatedRecords, insertedRecords) = pool.handleUpdatedRecords(records)

        if (!poolRepo.isPoolActiveByCoinCode(coinCode)) return false

        if (updatedRecords.isEmpty() && insertedRecords.isEmpty()) return false

        val updatedItems = updatedRecords.map { factory.createTransactionItem(coinCode, it) }
        val insertedItems = insertedRecords.map { factory.createTransactionItem(coinCode, it) }

        itemsDataSource.handleModifiedItems(updatedItems, insertedItems)

        return true
    }

    fun increasePage() {
        val unusedItems = mutableListOf<TransactionItem>()

        poolRepo.activePools.forEach { pool ->
            unusedItems.addAll(pool.unusedRecords.map { record ->
                factory.createTransactionItem(pool.coinCode, record)
            })
        }

        unusedItems.sortByDescending { it.record.timestamp }

        val usedItems = unusedItems.take(limit)

        itemsDataSource.add(usedItems)

        usedItems.forEach {
            poolRepo.getPool(it.coinCode)?.increaseFirstUnusedIndex()
        }
    }

    fun setCoinCodes(coinCodes: List<CoinCode>) {
        poolRepo.allPools.forEach {
            it.resetFirstUnusedIndex()
        }
        poolRepo.activatePools(coinCodes)
        itemsDataSource.clear()
    }

}

class TransactionItemFactory {
    fun createTransactionItem(coinCode: CoinCode, record: TransactionRecord): TransactionItem {
        return TransactionItem(coinCode, record)
    }
}

class TransactionItemDataSource {
    val count
        get() = items.size

    private val items = mutableListOf<TransactionItem>()

    fun clear() {
        items.clear()
    }

    fun add(items: List<TransactionItem>) {
        this.items.addAll(items)
    }

    fun itemForIndex(index: Int): TransactionItem = items[index]

    fun itemIndexesForTimestamp(coinCode: CoinCode, timestamp: Long): List<Int> {
        val indexes = mutableListOf<Int>()

        items.forEachIndexed { index, transactionItem ->
            if (transactionItem.coinCode == coinCode && transactionItem.record.timestamp == timestamp) {
                indexes.add(index)
            }
        }

        return indexes
    }

    fun handleModifiedItems(updatedItems: List<TransactionItem>, insertedItems: List<TransactionItem>) {
        items.removeAll(updatedItems)
        items.addAll(updatedItems)
        items.addAll(insertedItems)

        items.sortByDescending { it.record.timestamp }
    }

}

class PoolRepo {
    val activePools: List<Pool>
        get() = activePoolCoinCodes.mapNotNull { pools[it] }

    val allPools: List<Pool>
        get() = pools.values.toList()

    private var pools = mutableMapOf<CoinCode, Pool>()
    private var activePoolCoinCodes = listOf<CoinCode>()

    fun activatePools(coinCodes: List<CoinCode>) {
        coinCodes.forEach { coinCode ->
            if (!pools.containsKey(coinCode)) {
                pools[coinCode] = Pool(coinCode)
            }
        }

        this.activePoolCoinCodes = coinCodes
    }

    fun getPool(coinCode: CoinCode): Pool? {
        return pools[coinCode]
    }

    fun isPoolActiveByCoinCode(coinCode: CoinCode): Boolean {
        return activePoolCoinCodes.contains(coinCode)
    }

}

class Pool(val coinCode: CoinCode) {

    val records = mutableListOf<TransactionRecord>()

    val allShown: Boolean
        get() = allLoaded && unusedRecords.isEmpty()

    val unusedRecords: List<TransactionRecord>
        get() = when {
            records.isEmpty() -> listOf()
            else -> records.subList(firstUnusedIndex, records.size)
        }

    private var firstUnusedIndex = 0
    private var allLoaded = false

    fun increaseFirstUnusedIndex() {
        firstUnusedIndex++
    }

    fun resetFirstUnusedIndex() {
        firstUnusedIndex = 0
    }

    fun getFetchData(limit: Int): FetchData? {
        if (allLoaded) {
            return null
        }

        val unusedRecordsSize = unusedRecords.size
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

    fun handleUpdatedRecords(records: List<TransactionRecord>): Pair<List<TransactionRecord>, List<TransactionRecord>> {

        val updatedUsedRecords = mutableListOf<TransactionRecord>()
        val insertedUsedRecords = mutableListOf<TransactionRecord>()

        records.forEach { updatedRecord ->
            val updatedRecordIndex = this.records.indexOfFirst {
                it.transactionHash == updatedRecord.transactionHash
            }

            if (updatedRecordIndex != -1) {
                this.records[updatedRecordIndex] = updatedRecord

                if (updatedRecordIndex < firstUnusedIndex) {
                    updatedUsedRecords.add(updatedRecord)
                }
            } else {

                val nearestNextRecordIndex = this.records.indexOfFirst {
                    it.timestamp < updatedRecord.timestamp
                }

                if (nearestNextRecordIndex != -1) {
                    this.records.add(nearestNextRecordIndex, updatedRecord)

                    if (nearestNextRecordIndex < firstUnusedIndex) {
                        insertedUsedRecords.add(updatedRecord)
                        increaseFirstUnusedIndex()
                    }
                }
//                else if (this.records.isEmpty()) {
//                    this.records.add(updatedRecord)
//                    insertedUsedRecords.add(updatedRecord)
//                    increaseFirstUnusedIndex()
//                } else {
//                    allLoaded = false
//                }

            }
        }

        return Pair(updatedUsedRecords, insertedUsedRecords)
    }

}
