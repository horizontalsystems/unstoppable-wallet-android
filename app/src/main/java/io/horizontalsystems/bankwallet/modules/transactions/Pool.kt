package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import java.util.concurrent.CopyOnWriteArrayList

class Pool(val state: State) {

    class State(val wallet: Wallet) {
        val records: MutableList<TransactionRecord> = CopyOnWriteArrayList<TransactionRecord>()

        var firstUnusedIndex = 0
        var allLoaded = false

        val unusedRecords: List<TransactionRecord>
            get() = when {
                records.isEmpty() -> listOf()
                else -> records.subList(firstUnusedIndex, records.size)
            }

        fun add(records: List<TransactionRecord>) {
            this.records.addAll(records)
        }

        fun indexOf(record: TransactionRecord): Int {
            return records.indexOf(record)
        }

        fun insertIndexOf(record: TransactionRecord): Int {
            return records.indexOfFirst {
                it < record
            }
        }

        fun setRecord(index: Int, record: TransactionRecord) {
            records[index] = record
        }

        fun insertRecord(insertIndex: Int, record: TransactionRecord) {
            records.add(insertIndex, record)
        }

    }

    val records: MutableList<TransactionRecord>
        get() = state.records

    val allShown: Boolean
        get() = state.allLoaded && state.unusedRecords.isEmpty()

    val unusedRecords: List<TransactionRecord>
        get() = state.unusedRecords

    val wallet: Wallet
        get() = state.wallet

    fun increaseFirstUnusedIndex() {
        state.firstUnusedIndex++
    }

    fun resetFirstUnusedIndex() {
        state.firstUnusedIndex = 0
    }

    fun getFetchData(limit: Int): TransactionsModule.FetchData? {
        if (state.allLoaded) return null

        val unusedRecordsSize = state.unusedRecords.size
        if (unusedRecordsSize > limit) return null

        val recordFrom = state.records.lastOrNull()
        val fetchLimit = limit + 1 - unusedRecordsSize

        return TransactionsModule.FetchData(state.wallet, recordFrom, fetchLimit)
    }

    fun add(transactionRecords: List<TransactionRecord>) {
        if (transactionRecords.isEmpty()) {
            state.allLoaded = true
        } else {
            state.add(transactionRecords)
        }
    }

    fun handleUpdatedRecord(record: TransactionRecord): HandleResult {
        val updatedRecordIndex = state.indexOf(record)

        if (updatedRecordIndex != -1) {
            state.setRecord(updatedRecordIndex, record)

            if (updatedRecordIndex < state.firstUnusedIndex) {
                return HandleResult.UPDATED
            } else {
                return HandleResult.IGNORED
            }
        }

        val insertIndex = state.insertIndexOf(record)

        if (insertIndex != -1) {
            state.insertRecord(insertIndex, record)

            if (insertIndex < state.firstUnusedIndex) {
                increaseFirstUnusedIndex()
                return HandleResult.INSERTED
            } else if (insertIndex == 0) {
                return HandleResult.NEW_DATA
            } else {
                return HandleResult.IGNORED
            }
        } else if (state.allLoaded) {
            state.add(listOf(record))
            return HandleResult.NEW_DATA
        } else {
            return HandleResult.IGNORED
        }
    }

    enum class HandleResult {
        UPDATED, INSERTED, IGNORED, NEW_DATA
    }

}
