package io.horizontalsystems.bankwallet.core.adapters

import android.util.Log
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.zanokit.TransactionInfo
import io.horizontalsystems.zanokit.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.math.min

class ZanoTransactionsProvider(private val assetId: String) {
    private var transactions = listOf<TransactionInfo>()
    private val newTransactionsFlow = MutableSharedFlow<List<TransactionInfo>>(extraBufferCapacity = 1)

    fun onTransactions(all: List<TransactionInfo>) {
        val filtered = all.filter { it.assetId == assetId }
        Log.e("eee", "ZanoTransactionsProvider[$assetId] onTransactions: all=${all.size} filtered=${filtered.size}")
        val newTransactions = filtered.filter { tx ->
            transactions.none { it.uid == tx.uid && it.blockHeight == tx.blockHeight }
        }
        if (newTransactions.isNotEmpty()) {
            newTransactionsFlow.tryEmit(newTransactions)
            val notUpdated = transactions.filter { old -> newTransactions.none { new -> new.uid == old.uid } }
            transactions = (notUpdated + newTransactions).sortedByDescending { it.timestamp }
        }
    }

    suspend fun getTransactions(
        fromUid: String?,
        transactionType: FilterTransactionType,
        limit: Int,
    ): List<TransactionInfo> {
        val filters = getFilters(transactionType)
        val filtered = if (filters.isEmpty()) transactions else transactions.filter { tx -> filters.all { it(tx) } }
        val fromIndex = fromUid?.let { filtered.indexOfFirst { it.uid == fromUid } + 1 } ?: 0
        val result = filtered.subList(fromIndex, min(filtered.size, fromIndex + limit))
        Log.e("eee", "ZanoTransactionsProvider[$assetId] getTransactions: returning ${result.size} txs (cache=${transactions.size})")
        return result
    }

    fun getNewTransactionsFlow(transactionType: FilterTransactionType): Flow<List<TransactionInfo>> {
        val filters = getFilters(transactionType)
        return if (filters.isEmpty()) {
            newTransactionsFlow
        } else {
            newTransactionsFlow
                .map { txs -> txs.filter { tx -> filters.all { it(tx) } } }
                .filter { it.isNotEmpty() }
        }
    }

    private fun getFilters(transactionType: FilterTransactionType): List<(TransactionInfo) -> Boolean> =
        buildList {
            when (transactionType) {
                FilterTransactionType.All -> Unit
                FilterTransactionType.Incoming -> add { it.type == TransactionType.incoming }
                FilterTransactionType.Outgoing -> add { it.type == TransactionType.outgoing || it.type == TransactionType.sentToSelf }
                FilterTransactionType.Swap,
                FilterTransactionType.Approve -> add { false }
            }
        }
}
