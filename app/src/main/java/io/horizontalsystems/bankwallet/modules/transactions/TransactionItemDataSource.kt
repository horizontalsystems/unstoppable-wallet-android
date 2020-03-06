package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.TransactionRecord
import java.util.concurrent.CopyOnWriteArrayList

class TransactionItemDataSource {
    val items = CopyOnWriteArrayList<TransactionViewItem>()

    fun clear() {
        items.clear()
    }

    fun add(items: List<TransactionViewItem>) {
        this.items.addAll(items)
    }

    fun handleModifiedItems(updatedItems: List<TransactionViewItem>, insertedItems: List<TransactionViewItem>) {
        val tmpList = items.toMutableList()
        tmpList.removeAll(updatedItems)
        tmpList.addAll(updatedItems)
        tmpList.addAll(insertedItems)
        tmpList.sortDescending()

        items.clear()
        items.addAll(tmpList)
    }

    fun shouldInsertRecord(record: TransactionRecord): Boolean {
        return items.lastOrNull()?.record?.let { it < record } ?: true
    }

}
