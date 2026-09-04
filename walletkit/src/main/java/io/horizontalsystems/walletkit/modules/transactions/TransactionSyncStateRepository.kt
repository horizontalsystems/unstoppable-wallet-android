package io.horizontalsystems.walletkit.modules.transactions

import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.collectSafely
import io.horizontalsystems.walletkit.core.managers.TransactionAdapterManager
import io.horizontalsystems.walletkit.entities.LastBlockInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class TransactionSyncStateRepository(
    private val adapterManager: TransactionAdapterManager
) : Clearable {
    private val adapters = mutableMapOf<TransactionSource, ITransactionsAdapter>()

    private val _syncingFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val syncingFlow: Flow<Boolean> get() = _syncingFlow.distinctUntilChanged()

    // Bounded at 64 with newest-wins overflow. Last-block events are self-healing: every
    // source re-emits on each new block, so an event dropped under a burst is superseded by
    // that source's next update, while the bound prevents an unbounded backlog if the
    // collector stalls.
    private val _lastBlockInfoFlow = MutableSharedFlow<Pair<TransactionSource, LastBlockInfo>>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val lastBlockInfoFlow: Flow<Pair<TransactionSource, LastBlockInfo>> get() = _lastBlockInfoFlow

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun getLastBlockInfo(source: TransactionSource): LastBlockInfo? = adapters[source]?.lastBlockInfo

    fun setTransactionWallets(transactionWallets: List<TransactionWallet>) {
        coroutineScope.coroutineContext.cancelChildren()
        adapters.clear()

        transactionWallets.distinctBy { it.source }.forEach {
            val source = it.source
            adapterManager.getAdapter(source)?.let { adapter ->
                adapters[source] = adapter
            }
        }

        emitSyncing()

        adapters.forEach { (source, adapter) ->
            coroutineScope.launch {
                adapter.lastBlockUpdatedFlow.collectSafely {
                    adapter.lastBlockInfo?.let { lastBlockInfo ->
                        _lastBlockInfoFlow.tryEmit(Pair(source, lastBlockInfo))
                    }
                }
            }

            coroutineScope.launch {
                adapter.transactionsStateUpdatedFlow.collectSafely {
                    emitSyncing()
                }
            }
        }
    }

    private fun emitSyncing() {
        val syncing = adapters.any {
            it.value.transactionsState is AdapterState.Syncing
        }
        _syncingFlow.tryEmit(syncing)
    }

    override fun clear() {
        coroutineScope.cancel()
    }
}
