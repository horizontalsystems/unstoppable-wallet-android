package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.monerokit.model.TransactionInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.math.min

class MoneroTransactionsProvider {
    private var transactions = listOf<TransactionInfo>()
    private val newTransactionsFlow = MutableSharedFlow<List<TransactionInfo>>(extraBufferCapacity = 1)

    fun onTransactions(transactionInfos: List<TransactionInfo>) {
        val newTransactions = transactionInfos.filter { tx ->
            transactions.none { it.hash == tx.hash && it.blockheight == tx.blockheight && it.confirmations == tx.confirmations }
        }
        if (newTransactions.isNotEmpty()) {
            newTransactionsFlow.tryEmit(newTransactions)

            val notUpdatedTransactions = transactions.filter { old -> newTransactions.none { new -> new.hash == old.hash } }
            transactions = (notUpdatedTransactions + newTransactions).sortedByDescending { it.timestamp }
        }
    }

    suspend fun getTransactions(
        fromHash: String?,
        transactionType: FilterTransactionType,
        address: String?,
        limit: Int,
    ): List<TransactionInfo> {
        val filters = getFilters(transactionType)
        val filtered = when {
            filters.isEmpty() -> transactions
            else -> transactions.filter { tx -> filters.all { it.invoke(tx) } }
        }

        val fromIndex = fromHash?.let { filtered.indexOfFirst { it.hash == fromHash } + 1 } ?: 0

        return filtered.subList(fromIndex, min(filtered.size, fromIndex + limit))
    }

    fun getNewTransactionsFlow(transactionType: FilterTransactionType): Flow<List<TransactionInfo>> {
        val filters = getFilters(transactionType)

        return if (filters.isEmpty()) {
            newTransactionsFlow
        } else {
            newTransactionsFlow
                .map { txs ->
                    txs.filter { tx ->
                        filters.all { filter -> filter.invoke(tx) }
                    }
                }
                .filter { it.isNotEmpty() }
        }
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
