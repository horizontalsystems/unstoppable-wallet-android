package io.horizontalsystems.bankwallet.core.adapters.zcash

import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.TransactionRecipient
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.math.min

class ZcashTransactionsProvider(
    private val receiveAddress: String,
    private val synchronizer: SdkSynchronizer
) {

    private val transactions = mutableListOf<ZcashTransaction>()
    private val newTransactionsSubject = PublishSubject.create<List<ZcashTransaction>>()

    private fun getAllTransactionsSorted(): List<ZcashTransaction> {
        return transactions.sortedDescending()
    }

    @Synchronized
    fun onTransactions(transactionOverviews: List<TransactionOverview>) {
        synchronizer.coroutineScope.launch {
            val newTransactions = transactionOverviews.filter { tx ->
                transactions.none { it.rawId == tx.rawId }
            }

            if (newTransactions.isNotEmpty()) {
                val newZcashTransactions = newTransactions.map {
                    val recipient = if (it.isSentTransaction) {
                        synchronizer.getRecipients(it)
                            .filterIsInstance<TransactionRecipient.Address>()
                            .firstOrNull()
                            ?.addressValue
                    } else {
                        null
                    }

                    // sdk throws error when fetching memos
                    // val memo = synchronizer.getMemos(it).firstOrNull()

                    ZcashTransaction(it, recipient, null)
                }
                newTransactionsSubject.onNext(newZcashTransactions)
                transactions.addAll(newZcashTransactions)
            }
        }
    }

    fun getNewTransactionsFlowable(transactionType: FilterTransactionType): Flowable<List<ZcashTransaction>> {
        val observable = when (transactionType) {
            FilterTransactionType.All -> newTransactionsSubject
            FilterTransactionType.Incoming -> {
                newTransactionsSubject
                    .map { it.filter { it.isIncoming } }
                    .filter { it.isNotEmpty() }
            }
            FilterTransactionType.Outgoing -> {
                newTransactionsSubject
                    .map { it.filter { !it.isIncoming } }
                    .filter { it.isNotEmpty() }
            }
            FilterTransactionType.Swap,
            FilterTransactionType.Approve -> Observable.empty()
        }

        return observable.toFlowable(BackpressureStrategy.BUFFER)
    }

    @Synchronized
    fun getTransactions(
        from: Triple<ByteArray, Long, Int>?,
        transactionType: FilterTransactionType,
        limit: Int,
    ): Single<List<ZcashTransaction>> {
        return when (transactionType) {
            FilterTransactionType.All -> getTxsFiltered(from, limit, null)
            FilterTransactionType.Incoming -> getTxsFiltered(from, limit, true)
            FilterTransactionType.Outgoing -> getTxsFiltered(from, limit, false)
            FilterTransactionType.Swap,
            FilterTransactionType.Approve, -> Single.just(listOf())
        }
    }

    private fun getTxsFiltered(from: Triple<ByteArray, Long, Int>?, limit: Int, incoming: Boolean?): Single<List<ZcashTransaction>> = Single.create { emitter ->
        try {
            val transactions = when (incoming) {
                true -> getAllTransactionsSorted().filter { it.isIncoming }
                false -> getAllTransactionsSorted().filter { !it.isIncoming }
                null -> getAllTransactionsSorted()
            }

            val fromIndex = from?.let { (transactionHash, timestamp, transactionIndex) ->
                transactions.indexOfFirst { it.transactionHash.contentEquals(transactionHash) && it.timestamp == timestamp && it.transactionIndex == transactionIndex } + 1
            } ?: 0

            emitter.onSuccess(transactions.subList(fromIndex, min(transactions.size, fromIndex + limit)))
        } catch (error: Throwable) {
            emitter.onError(error)
        }
    }
}
