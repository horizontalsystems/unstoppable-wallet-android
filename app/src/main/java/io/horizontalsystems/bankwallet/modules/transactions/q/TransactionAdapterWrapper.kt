package io.horizontalsystems.bankwallet.modules.transactions.q

import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionWallet
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class TransactionAdapterWrapper(
    private val transactionsAdapter: ITransactionsAdapter,
    private val transactionWallet: TransactionWallet
) {

    private val updatedSubject = PublishSubject.create<Unit>()
    val updatedObservable: Observable<Unit> get() = updatedSubject

    private var lastUsed: TransactionRecord? = null
    private val transactionRecords = mutableListOf<TransactionRecord>()
    private var allLoaded = false
    private val disposables = CompositeDisposable()

    private fun getRecordsUnused(): List<TransactionRecord> {
        val fromIndex = when {
            lastUsed == null -> 0
            else -> transactionRecords.indexOf(lastUsed) + 1
        }

        return transactionRecords.subList(fromIndex, transactionRecords.size)
    }

    fun start() {
        transactionsAdapter.getTransactionRecordsFlowable(transactionWallet.coin)
            .subscribeIO {
                handleUpdatedRecords(it)
            }
            .let {
                disposables.add(it)
            }
    }

    private fun handleUpdatedRecords(records: List<TransactionRecord>) {
        var needToUpdate = false

        records.sortedDescending().forEach { updatedRecord ->
            val indexOfUpdated = transactionRecords.indexOf(updatedRecord)

            if (indexOfUpdated != -1) {
                transactionRecords[indexOfUpdated] = updatedRecord
            } else {
                val insertIndex = transactionRecords.indexOfFirst {
                    it < updatedRecord
                }

                if (insertIndex != -1) {
                    transactionRecords.add(insertIndex, updatedRecord)
                } else if (allLoaded) {
                    transactionRecords.add(updatedRecord)
                    lastUsed = updatedRecord
                }
            }

            needToUpdate = needToUpdate || lastUsed?.let { updatedRecord >= it } ?: false
        }

        if (needToUpdate) {
            updatedSubject.onNext(Unit)
        }
    }

    fun stop() {
        disposables.clear()
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

}
