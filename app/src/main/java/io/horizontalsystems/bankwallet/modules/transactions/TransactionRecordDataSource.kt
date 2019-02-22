package io.horizontalsystems.bankwallet.modules.transactions

import android.support.v7.util.DiffUtil
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


    fun itemIndexesForPending(coinCode: CoinCode, thresholdBlockHeight: Int): List<Int> =
            itemsDataSource.itemIndexesForPending(coinCode, thresholdBlockHeight)

    fun getFetchDataList(): List<FetchData> = poolRepo.activePools.mapNotNull {
        it.getFetchData(limit)
    }

    fun handleNextRecords(records: Map<CoinCode, List<TransactionRecord>>) {
        records.forEach { (coinCode, transactionRecords) ->
            poolRepo.getPool(coinCode)?.add(transactionRecords)
        }
    }

    fun handleUpdatedRecords(records: List<TransactionRecord>, coinCode: CoinCode): DiffUtil.DiffResult? {
        val pool = poolRepo.getPool(coinCode) ?: return null

        val updatedRecords = mutableListOf<TransactionRecord>()
        val insertedRecords = mutableListOf<TransactionRecord>()

        records.forEach {
            when (pool.handleUpdatedRecord(it)) {
                Pool.HandleResult.UPDATED -> updatedRecords.add(it)
                Pool.HandleResult.INSERTED -> insertedRecords.add(it)
                Pool.HandleResult.NEW_DATA -> {
                    if (itemsDataSource.shouldInsertRecord(it)) {
                        insertedRecords.add(it)
                        pool.increaseFirstUnusedIndex()
                    }
                }
                Pool.HandleResult.IGNORED -> {
                }
            }
        }

        if (!poolRepo.isPoolActiveByCoinCode(coinCode)) return null

        val updatedItems = updatedRecords.map { factory.createTransactionItem(coinCode, it) }
        val insertedItems = insertedRecords.map { factory.createTransactionItem(coinCode, it) }

        return itemsDataSource.handleModifiedItems(updatedItems, insertedItems)
    }

    fun increasePage(): Int {
        val unusedItems = mutableListOf<TransactionItem>()

        poolRepo.activePools.forEach { pool ->
            unusedItems.addAll(pool.unusedRecords.map { record ->
                factory.createTransactionItem(pool.coinCode, record)
            })
        }

        if (unusedItems.isEmpty()) return 0

        unusedItems.sortByDescending { it.record.timestamp }

        val usedItems = unusedItems.take(limit)

        itemsDataSource.add(usedItems)

        usedItems.forEach {
            poolRepo.getPool(it.coinCode)?.increaseFirstUnusedIndex()
        }

        return usedItems.size
    }

    fun setCoinCodes(coinCodes: List<CoinCode>) {
        poolRepo.allPools.forEach {
            it.resetFirstUnusedIndex()
        }
        poolRepo.activatePools(coinCodes)
        itemsDataSource.clear()
    }
}

