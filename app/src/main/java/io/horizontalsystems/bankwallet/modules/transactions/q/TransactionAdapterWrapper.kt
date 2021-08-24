package io.horizontalsystems.bankwallet.modules.transactions.q

import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionWallet
import io.reactivex.Observable
import io.reactivex.Single

class TransactionAdapterWrapper(
    private val transactionsAdapter: ITransactionsAdapter,
    private val transactionWallet: TransactionWallet
) {
    val updatedObservable: Observable<List<TransactionRecord>> get() = transactionsAdapter.getTransactionRecordsFlowable(transactionWallet.coin).toObservable()

    private var lastUsed: TransactionRecord? = null
    private val transactionRecords = mutableListOf<TransactionRecord>()
    private var allLoaded = false

    private fun getRecordsUnused(): List<TransactionRecord> {
        val fromIndex = when {
            lastUsed == null -> 0
            else -> transactionRecords.indexOf(lastUsed) + 1
        }

        return transactionRecords.subList(fromIndex, transactionRecords.size)
    }

    fun getNext(limit: Int): Single<List<TransactionRecord>> {
        val recordsUnused = getRecordsUnused()

        return if (recordsUnused.size >= limit || allLoaded) {
            Single.just(recordsUnused.take(limit))
        } else {
            val numberOfRecordsToRequest = limit - recordsUnused.size

            transactionsAdapter.getTransactionsAsync(transactionRecords.lastOrNull(), transactionWallet.coin, numberOfRecordsToRequest)
                .map {
                    allLoaded = it.size < numberOfRecordsToRequest
                    transactionRecords.addAll(it)

                    getRecordsUnused().take(limit)
                }
        }
    }

    fun markUsed(record: TransactionRecord?) {
        lastUsed = record
    }

    fun markUpdated(updatedRecord: TransactionRecord) {
        val indexOfUpdated = transactionRecords.indexOf(updatedRecord)
        if (indexOfUpdated != -1) {
            transactionRecords[indexOfUpdated] = updatedRecord
        }
    }

    fun markInserted(updatedRecord: TransactionRecord) {
        val indexToInsert = transactionRecords.indexOfFirst { it < updatedRecord }
        if (indexToInsert != -1) {
            transactionRecords.add(indexToInsert, updatedRecord)
        } else {
            transactionRecords.add(updatedRecord)
            lastUsed = updatedRecord
        }
    }

    fun markIgnored(updatedRecord: TransactionRecord) {
        allLoaded = false
    }
}
