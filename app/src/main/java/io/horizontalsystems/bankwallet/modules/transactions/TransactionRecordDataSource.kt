package io.horizontalsystems.bankwallet.modules.transactions

import androidx.recyclerview.widget.DiffUtil
import io.horizontalsystems.bankwallet.entities.Coin
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

    val allRecords: Map<Coin, List<TransactionRecord>>
        get() = poolRepo.activePools.map {
            Pair(it.coin, it.records)
        }.toMap()

    fun itemForIndex(index: Int): TransactionItem =
            itemsDataSource.itemForIndex(index)

    fun itemIndexesForTimestamp(coin: Coin, timestamp: Long): List<Int> =
            itemsDataSource.itemIndexesForTimestamp(coin, timestamp)


    fun itemIndexesForPending(coin: Coin, thresholdBlockHeight: Int): List<Int> =
            itemsDataSource.itemIndexesForPending(coin, thresholdBlockHeight)

    fun getFetchDataList(): List<FetchData> = poolRepo.activePools.mapNotNull {
        it.getFetchData(limit)
    }

    fun handleNextRecords(records: Map<Coin, List<TransactionRecord>>) {
        records.forEach { (coin, transactionRecords) ->
            poolRepo.getPool(coin)?.add(transactionRecords)
        }
    }

    fun handleUpdatedRecords(records: List<TransactionRecord>, coin: Coin): DiffUtil.DiffResult? {
        val pool = poolRepo.getPool(coin) ?: return null

        val updatedRecords = mutableListOf<TransactionRecord>()
        val insertedRecords = mutableListOf<TransactionRecord>()

        records.sorted().forEach {
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

        if (!poolRepo.isPoolActiveByCoinCode(coin)) return null

        val updatedItems = updatedRecords.map { factory.createTransactionItem(coin, it) }
        val insertedItems = insertedRecords.map { factory.createTransactionItem(coin, it) }

        return itemsDataSource.handleModifiedItems(updatedItems, insertedItems)
    }

    fun increasePage(): Int {
        val unusedItems = mutableListOf<TransactionItem>()

        poolRepo.activePools.forEach { pool ->
            unusedItems.addAll(pool.unusedRecords.map { record ->
                factory.createTransactionItem(pool.coin, record)
            })
        }

        if (unusedItems.isEmpty()) return 0

        unusedItems.sortDescending()

        val usedItems = unusedItems.take(limit)

        itemsDataSource.add(usedItems)

        usedItems.forEach {
            poolRepo.getPool(it.coin)?.increaseFirstUnusedIndex()
        }

        return usedItems.size
    }

    fun setCoinCodes(coins: List<Coin>) {
        poolRepo.allPools.forEach {
            it.resetFirstUnusedIndex()
        }
        poolRepo.activatePools(coins)
        itemsDataSource.clear()
    }
}

