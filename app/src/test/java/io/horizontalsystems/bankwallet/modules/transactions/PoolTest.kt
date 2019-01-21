package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.TransactionRecord
import org.junit.Assert
import org.junit.Test

class PoolTest {

    @Test
    fun handleUpdatedRecords() {
        val pool = Pool("BTC")

        val initialTransactionRecords = List(10) {
            TransactionRecord("$it").apply {
                timestamp = 10L * it
            }
        }.sortedByDescending { it.timestamp }

        pool.add(initialTransactionRecords)

        val lastUsedRecordTimestamp = 50L

        initialTransactionRecords.iterator().let {
            while (it.next().timestamp >= lastUsedRecordTimestamp) {
                pool.increaseFirstUnusedIndex()
            }
        }

        val newTransactionRecordInWindow = TransactionRecord("new1").apply {
            timestamp = lastUsedRecordTimestamp + 1
        }

        val newTransactionRecordOutOfWindow = TransactionRecord("new2").apply {
            timestamp = lastUsedRecordTimestamp - 1
        }

        val updatedRecords = listOf(newTransactionRecordInWindow, newTransactionRecordOutOfWindow, initialTransactionRecords[2], initialTransactionRecords[4], initialTransactionRecords[5])

        val (actualUpdatedRecords, actualInsertedRecords) = pool.handleUpdatedRecords(updatedRecords)

        Assert.assertArrayEquals(arrayOf(newTransactionRecordInWindow), actualInsertedRecords.toTypedArray())
        Assert.assertArrayEquals(arrayOf(initialTransactionRecords[2], initialTransactionRecords[4]), actualUpdatedRecords.toTypedArray())
    }

}
