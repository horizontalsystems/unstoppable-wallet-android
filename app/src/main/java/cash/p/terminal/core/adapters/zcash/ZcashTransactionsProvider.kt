package cash.p.terminal.core.adapters.zcash

import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.TransactionRecipient
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.min

class ZcashTransactionsProvider(private val synchronizer: SdkSynchronizer) {

    private var transactions = listOf<ZcashTransaction>()
    private val newTransactionsFlow = MutableSharedFlow<List<ZcashTransaction>>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @Synchronized
    fun onTransactions(transactionOverviews: List<TransactionOverview>) {
        synchronizer.coroutineScope.launch {
            val newTransactions = transactionOverviews.filter { tx ->
                transactions.none { it.rawId == tx.txId.value }
            }

            if (newTransactions.isNotEmpty()) {
                val newZcashTransactions = newTransactions.map {
                    val recipient = if (it.isSentTransaction) {
                        synchronizer.getRecipients(it)
                            .filterIsInstance<TransactionRecipient>()
                            .firstOrNull()
                            ?.addressValue
                    } else {
                        null
                    }

                    // sdk throws error when fetching memos
                    // val memo = synchronizer.getMemos(it).firstOrNull()

                    ZcashTransaction(it, recipient, null)
                }
                transactions = (transactions + newZcashTransactions).sortedDescending()
                newTransactionsFlow.emit(newZcashTransactions)
            }
        }
    }

    fun getNewTransactionsFlowable(
        transactionType: FilterTransactionType,
        address: String?
    ): Flow<List<ZcashTransaction>> {
        val filters = getFilters(transactionType, address)

        return if (filters.isEmpty()) {
            newTransactionsFlow
        } else {
            newTransactionsFlow.map { txs ->
                txs.filter { tx ->
                    filters.all { filter -> filter.invoke(tx) }
                }
            }.filter {
                it.isNotEmpty()
            }
        }
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

    fun getTransactions(
        from: Triple<ByteArray, Long, Int>?,
        transactionType: FilterTransactionType,
        address: String?,
        limit: Int,
    ) = try {
        val filters = getFilters(transactionType, address)
        val filtered = when {
            filters.isEmpty() -> transactions
            else -> transactions.filter { tx -> filters.all { it.invoke(tx) } }
        }

        val fromIndex = from?.let { (transactionHash, timestamp, transactionIndex) ->
            filtered.indexOfFirst { it.transactionHash.contentEquals(transactionHash) && it.timestamp == timestamp && it.transactionIndex == transactionIndex } + 1
        } ?: 0

        filtered.subList(fromIndex, min(filtered.size, fromIndex + limit))
    } catch (error: Throwable) {
        emptyList<ZcashTransaction>()
    }
}
