package io.horizontalsystems.bankwallet.core.adapters.zcash

import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.db.entity.PendingTransaction
import cash.z.ecc.android.sdk.db.entity.hasRawTransactionId
import cash.z.ecc.android.sdk.db.entity.isMined
import cash.z.ecc.android.sdk.ext.collectWith
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.min

class ZcashTransactionsProvider(synchronizer: Synchronizer) {

    init {
        synchronizer.clearedTransactions.distinctUntilChanged().collectWith(GlobalScope, ::onClearedTransactions)
        synchronizer.pendingTransactions.distinctUntilChanged().collectWith(GlobalScope, ::onPendingTransactions)
    }

    private var confirmedTransactions: MutableList<ZcashTransaction> = mutableListOf()
    private var pendingTransactions: MutableList<ZcashTransaction> = mutableListOf()
    private val newTransactionsSubject: PublishSubject<List<ZcashTransaction>> = PublishSubject.create()

    private fun getAllTransactionsSorted(): List<ZcashTransaction> {
        return confirmedTransactions.union(pendingTransactions).sortedDescending()
    }

    @Synchronized
    private fun onClearedTransactions(transactions: List<ConfirmedTransaction>) {
        val newTransactions = transactions.filter { tx -> tx.minedHeight > 0 && !confirmedTransactions.any { it.id == tx.id } }

        if (newTransactions.isNotEmpty()) {
            val newZcashTransactions = newTransactions.map { ZcashTransaction(it) }
            newTransactionsSubject.onNext(newZcashTransactions)
            confirmedTransactions.addAll(newZcashTransactions)
        }

    }

    @Synchronized
    private fun onPendingTransactions(transactions: List<PendingTransaction>) {
        val newTransactions = transactions.filter { tx -> tx.hasRawTransactionId() && !tx.isMined() && !pendingTransactions.any { it.id == tx.id } }

        if (newTransactions.isNotEmpty()) {
            val newZcashTransactions = newTransactions.map { ZcashTransaction(it) }
            newTransactionsSubject.onNext(newZcashTransactions)
            pendingTransactions.addAll(newZcashTransactions)
        }
    }

    val newTransactionsFlowable: Flowable<List<ZcashTransaction>>
        get() = newTransactionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    @Synchronized
    fun getTransactions(from: Triple<ByteArray, Long, Int>?, limit: Int): Single<List<ZcashTransaction>> =
            Single.create { emitter ->
                try {
                    val allTransactions = getAllTransactionsSorted()
                    val fromIndex: Int = if (from != null) {
                        val transactionHash = from.first
                        val timestamp = from.second
                        val transactionIndex = from.third
                        allTransactions.indexOfFirst { it.transactionHash.contentEquals(transactionHash) && it.timestamp == timestamp && it.transactionIndex == transactionIndex } + 1
                    } else 0

                    emitter.onSuccess(allTransactions.subList(fromIndex, min(allTransactions.size, fromIndex + limit)))

                } catch (error: Throwable) {
                    emitter.onError(error)
                }
            }
}
