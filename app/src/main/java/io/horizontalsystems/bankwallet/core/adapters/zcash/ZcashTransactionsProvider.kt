package io.horizontalsystems.bankwallet.core.adapters.zcash

import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.TransactionRecipient
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
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

    private var transactions = listOf<ZcashTransaction>()
    private val newTransactionsSubject = PublishSubject.create<List<ZcashTransaction>>()

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
                transactions = (transactions + newZcashTransactions).sortedDescending()
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
            FilterTransactionType.Approve,
            -> add { false }
        }

        if (address != null) {
            add {
                it.toAddress?.lowercase() == address.lowercase()
            }
        }
    }

    @Synchronized
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
