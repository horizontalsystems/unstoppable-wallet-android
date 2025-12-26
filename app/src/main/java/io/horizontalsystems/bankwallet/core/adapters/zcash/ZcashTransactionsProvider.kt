package io.horizontalsystems.bankwallet.core.adapters.zcash

import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.TransactionRecipient
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min

class ZcashTransactionsProvider(
    private val accountUuid: AccountUuid,
    private val synchronizer: SdkSynchronizer
) {
    private val mutex = Mutex()
    private var transactions = listOf<ZcashTransaction>()
    private val newTransactionsSubject = PublishSubject.create<List<ZcashTransaction>>()

    fun onTransactions(transactionOverviews: List<TransactionOverview>) {
        synchronizer.coroutineScope.launch {
            mutex.withLock {
                val newTransactions = transactionOverviews.filter { tx ->
                    transactions.none { it.transactionHash.contentEquals(tx.txId.value.byteArray) && it.minedHeight == tx.minedHeight?.value }
                }

                if (newTransactions.isNotEmpty()) {
                    val newZcashTransactions = newTransactions.map {
                        val recipients = if (it.isSentTransaction) {
                            synchronizer.getRecipients(it)
                                .filterIsInstance<TransactionRecipient>()
                                .toList()
                        } else {
                            null
                        }
                        val memo = synchronizer.getMemos(it).firstOrNull()
                        ZcashTransaction(accountUuid, it, recipients, memo)
                    }
                    newTransactionsSubject.onNext(newZcashTransactions)
                    val notUpdatedTransactions =
                        transactions.filter { old -> newZcashTransactions.none { new -> new.transactionHash.contentEquals(old.transactionHash) } }
                    transactions = (notUpdatedTransactions + newZcashTransactions).sortedDescending()
                }
            }
        }
    }

    fun getNewTransactionsFlowable(transactionType: FilterTransactionType, address: String?): Flowable<List<ZcashTransaction>> {
        val filters = getFilters(transactionType, address)

        val observable = if (filters.isEmpty()) {
            newTransactionsSubject
        } else {
            newTransactionsSubject.map { txs ->
                txs.filter { tx ->
                    filters.all { filter -> filter.invoke(tx) }
                }
            }.filter {
                it.isNotEmpty()
            }
        }

        return observable.toFlowable(BackpressureStrategy.LATEST)
    }

    private fun getFilters(
        transactionType: FilterTransactionType,
        address: String?,
    ) = buildList<(ZcashTransaction) -> Boolean> {
        when (transactionType) {
            FilterTransactionType.All -> Unit
            FilterTransactionType.Incoming -> add { it.isIncoming }
            FilterTransactionType.Outgoing -> add { !it.isIncoming }
            FilterTransactionType.Swap,
            FilterTransactionType.Approve -> add { false }
        }

        if (address != null) {
            add { tx ->
                tx.recipients?.any { it.addressValue == address } ?: false
            }
        }
    }

    fun getTransactions(
        from: Triple<ByteArray, Long, Int>?,
        transactionType: FilterTransactionType,
        address: String?,
        limit: Int,
    ) = Single.create { emitter ->
        try {
            val filters = getFilters(transactionType, address)
            val filtered = when {
                filters.isEmpty() -> transactions
                else -> transactions.filter { tx -> filters.all { it.invoke(tx) } }
            }

            val fromIndex = from?.let { (transactionHash, timestamp, transactionIndex) ->
                filtered.indexOfFirst { it.transactionHash.contentEquals(transactionHash) && it.timestamp == timestamp && it.transactionIndex == transactionIndex } + 1
            } ?: 0

            emitter.onSuccess(filtered.subList(fromIndex, min(filtered.size, fromIndex + limit)))
        } catch (error: Throwable) {
            emitter.onError(error)
        }
    }
}
