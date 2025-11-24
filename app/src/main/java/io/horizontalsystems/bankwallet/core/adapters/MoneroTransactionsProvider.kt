package io.horizontalsystems.bankwallet.core.adapters

import android.util.Log
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.monerokit.model.TransactionInfo
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import kotlin.math.min

class MoneroTransactionsProvider {
    private var transactions = listOf<TransactionInfo>()
    private val newTransactionsSubject = PublishSubject.create<List<TransactionInfo>>()

    fun onTransactions(transactionInfos: List<TransactionInfo>) {
        val newTransactions = transactionInfos.filter { tx ->
            transactions.none { it.hash == tx.hash && it.blockheight == tx.blockheight && it.confirmations == tx.confirmations }
        }
        if (newTransactions.isNotEmpty()) {
            newTransactionsSubject.onNext(newTransactions)

            val notUpdatedTransactions = transactions.filter { old -> newTransactions.none { new -> new.hash == old.hash } }
            transactions = (notUpdatedTransactions + newTransactions).sortedByDescending { it.timestamp }
        }
    }

    fun getTransactions(
        fromHash: String?,
        transactionType: FilterTransactionType,
        address: String?,
        limit: Int,
    ) = Single.create { emitter ->
        try {
            val filters = getFilters(transactionType)
            val filtered = when {
                filters.isEmpty() -> transactions
                else -> transactions.filter { tx -> filters.all { it.invoke(tx) } }
            }

            val fromIndex = fromHash?.let { filtered.indexOfFirst { it.hash == fromHash } + 1 } ?: 0

            emitter.onSuccess(filtered.subList(fromIndex, min(filtered.size, fromIndex + limit)))
        } catch (error: Throwable) {
            emitter.onError(error)
        }
    }


    fun getNewTransactionsFlowable(transactionType: FilterTransactionType): Flowable<List<TransactionInfo>> {
        val filters = getFilters(transactionType)

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

    private fun getFilters(transactionType: FilterTransactionType) =
        buildList<(TransactionInfo) -> Boolean> {
            when (transactionType) {
                FilterTransactionType.All -> Unit
                FilterTransactionType.Incoming -> add { it.direction == TransactionInfo.Direction.Direction_In }
                FilterTransactionType.Outgoing -> add { it.direction == TransactionInfo.Direction.Direction_Out }
                FilterTransactionType.Swap,
                FilterTransactionType.Approve -> add { false }
            }
        }

}
