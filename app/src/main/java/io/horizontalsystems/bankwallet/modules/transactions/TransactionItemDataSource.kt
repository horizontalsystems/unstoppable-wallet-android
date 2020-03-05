package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import java.util.concurrent.CopyOnWriteArrayList

class TransactionItemDataSource {
    val count
        get() = items.size

    private val items = CopyOnWriteArrayList<TransactionViewItem>()

    fun clear() {
        items.clear()
    }

    fun add(items: List<TransactionViewItem>) {
        this.items.addAll(items)
    }

    fun itemForIndex(index: Int): TransactionViewItem = items[index]

    fun itemIndexesForTimestamp(coin: Coin, timestamp: Long): List<Int> {
        val indexes = mutableListOf<Int>()

        items.forEachIndexed { index, transactionItem ->
            if (transactionItem.wallet.coin == coin && transactionItem.record.timestamp == timestamp) {
                indexes.add(index)
            }
        }

        return indexes
    }

    fun itemIndexesForPending(wallet: Wallet, thresholdBlockHeight: Int): List<Int> {
        val indexes = mutableListOf<Int>()

        items.forEachIndexed { index, item ->
            if (item.wallet == wallet && (item.record.blockHeight == null || item.record.blockHeight >= thresholdBlockHeight)) {
                indexes.add(index)
            }
        }

        return indexes
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

    fun itemIndexesForLocked(wallet: Wallet, unlockingBefore: Long, oldBlockTimestamp: Long?): List<Int> {
        val indexes = mutableListOf<Int>()

        items.forEachIndexed { index, item ->
            if (item.wallet == wallet && item.record.lockInfo != null &&
                    item.record.lockInfo.lockedUntil.time / 1000 > oldBlockTimestamp ?: 0 &&
                    item.record.lockInfo.lockedUntil.time / 1000 <= unlockingBefore) {
                indexes.add(index)
            }
        }

        return indexes
    }

}
