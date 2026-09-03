package io.horizontalsystems.walletkit.core.adapters.zcash

import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.model.AccountUuid
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.TransactionRecipient
import io.horizontalsystems.walletkit.modules.transactions.FilterTransactionType
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min

class ZcashTransactionsProvider(
    private val accountUuid: AccountUuid,
    private val synchronizer: SdkSynchronizer,
    private val isMigrationTransaction: (txHash: ByteArray) -> Boolean
) {
    private val mutex = Mutex()
    private var transactions = listOf<ZcashTransaction>()
    private val newTransactionsFlow = MutableSharedFlow<List<ZcashTransaction>>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

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
                        val outputs = if (it.isSentTransaction) {
                            synchronizer.getTransactionOutputs(it)
                        } else {
                            emptyList()
                        }
                        ZcashTransaction(accountUuid, it, recipients, memo, isMigrationTransaction(it.txId.value.byteArray), outputs)
                    }
                    newTransactionsFlow.tryEmit(newZcashTransactions)
                    val notUpdatedTransactions =
                        transactions.filter { old -> newZcashTransactions.none { new -> new.transactionHash.contentEquals(old.transactionHash) } }
                    transactions = (notUpdatedTransactions + newZcashTransactions).sortedDescending()
                }
            }
        }
    }

    fun getNewTransactionsFlow(transactionType: FilterTransactionType, address: String?): Flow<List<ZcashTransaction>> {
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
            FilterTransactionType.Approve -> add { false }
        }

        if (address != null) {
            add { tx ->
                tx.recipients?.any { it.addressValue == address } ?: false
            }
        }
    }

    suspend fun getTransactions(
        from: Triple<ByteArray, Long, Int>?,
        transactionType: FilterTransactionType,
        address: String?,
        limit: Int,
    ): List<ZcashTransaction> {
        val filters = getFilters(transactionType, address)
        val filtered = when {
            filters.isEmpty() -> transactions
            else -> transactions.filter { tx -> filters.all { it.invoke(tx) } }
        }

        val fromIndex = from?.let { (transactionHash, timestamp, transactionIndex) ->
            filtered.indexOfFirst { it.transactionHash.contentEquals(transactionHash) && it.timestamp == timestamp && it.transactionIndex == transactionIndex } + 1
        } ?: 0

        return filtered.subList(fromIndex, min(filtered.size, fromIndex + limit))
    }
}
