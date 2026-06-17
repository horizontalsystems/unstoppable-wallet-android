package cash.p.terminal.modules.transactions

import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Clearable
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.wallet.transaction.TransactionSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TransactionSyncStateRepository(
    private val adapterManager: TransactionAdapterManager
) : Clearable {
    private val adapters = mutableMapOf<TransactionSource, ITransactionsAdapter>()
    private val adaptersMutex = Mutex()
    @Volatile
    private var requestedSourceCount = 0

    private val _syncingFlow = MutableStateFlow(true)
    val syncingFlow: StateFlow<Boolean> = _syncingFlow.asStateFlow()

    private val _lastBlockInfoFlow = MutableSharedFlow<Pair<TransactionSource, LastBlockInfo>>()
    val lastBlockInfoFlow: SharedFlow<Pair<TransactionSource, LastBlockInfo>> = _lastBlockInfoFlow.asSharedFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var setupJob: Job? = null

    suspend fun getLastBlockInfo(source: TransactionSource): LastBlockInfo? =
        adaptersMutex.withLock { adapters[source]?.lastBlockInfo }

    fun setTransactionWallets(transactionWallets: List<TransactionWallet>) {
        val distinctWallets = transactionWallets.distinctBy { it.source }
        requestedSourceCount = distinctWallets.size
        _syncingFlow.value = true

        setupJob?.cancel()
        setupJob = coroutineScope.launch {
            val newAdapters = mutableMapOf<TransactionSource, ITransactionsAdapter>()
            distinctWallets.forEach { wallet ->
                val source = wallet.source
                adapterManager.getAdapter(source)?.let { adapter ->
                    newAdapters[source] = adapter
                }
            }

            adaptersMutex.withLock {
                adapters.clear()
                adapters.putAll(newAdapters)
            }

            emitSyncing()
            monitor(newAdapters)
        }
    }

    private suspend fun monitor(adapterMap: Map<TransactionSource, ITransactionsAdapter>) {
        supervisorScope {
            launch {
                adapterManager.initializationFlow.collect {
                    emitSyncing()
                }
            }

            adapterMap.forEach { (source, adapter) ->
                launch {
                    adapter.lastBlockUpdatedFlowable.asFlow().collect {
                        adapter.lastBlockInfo?.let { lastBlockInfo ->
                            _lastBlockInfoFlow.emit(Pair(source, lastBlockInfo))
                        }
                    }
                }

                launch {
                    adapter.transactionsStateUpdatedFlowable.asFlow().collect {
                        emitSyncing()
                    }
                }
            }
        }
    }

    private suspend fun emitSyncing() {
        val syncing = adaptersMutex.withLock {
            when {
                requestedSourceCount == 0 -> false
                adapters.size < requestedSourceCount && !adapterManager.initializationFlow.value -> true
                else -> adapters.any {
                    val state = it.value.transactionsState
                    state is AdapterState.Syncing ||
                        state is AdapterState.Connecting ||
                        state is AdapterState.SearchingTxs
                }
            }
        }
        _syncingFlow.value = syncing
    }

    override fun clear() {
        setupJob?.cancel()
        requestedSourceCount = 0
        _syncingFlow.value = false
    }
}
