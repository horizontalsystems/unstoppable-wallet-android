package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.TransactionItem
import io.horizontalsystems.bankwallet.entities.TransactionRecord

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

    fun shouldInsertRecord(record: TransactionRecord): Boolean {
        return items.lastOrNull()?.record?.let { it.timestamp < record.timestamp } ?: true
    }

}
