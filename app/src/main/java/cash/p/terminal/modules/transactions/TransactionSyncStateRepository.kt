package cash.p.terminal.modules.transactions

import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Clearable
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.wallet.transaction.TransactionSource
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    private val syncingSubject = PublishSubject.create<Boolean>()
    val syncingObservable: Observable<Boolean> get() = syncingSubject.distinctUntilChanged()

    private val lastBlockInfoSubject = PublishSubject.create<Pair<TransactionSource, LastBlockInfo>>()
    val lastBlockInfoObservable: Observable<Pair<TransactionSource, LastBlockInfo>> get() = lastBlockInfoSubject

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null

    suspend fun getLastBlockInfo(source: TransactionSource): LastBlockInfo? =
        adaptersMutex.withLock { adapters[source]?.lastBlockInfo }

    fun setTransactionWallets(transactionWallets: List<TransactionWallet>) {
        monitoringJob?.cancel()

        coroutineScope.launch {
            val newAdapters = mutableMapOf<TransactionSource, ITransactionsAdapter>()
            transactionWallets.distinctBy { it.source }.forEach { wallet ->
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
            startMonitoring(newAdapters)
        }
    }

    private fun startMonitoring(adapterMap: Map<TransactionSource, ITransactionsAdapter>) {
        monitoringJob = coroutineScope.launch {
            supervisorScope {
                adapterMap.forEach { (source, adapter) ->
                    launch {
                        adapter.lastBlockUpdatedFlowable.asFlow().collect {
                            adapter.lastBlockInfo?.let { lastBlockInfo ->
                                lastBlockInfoSubject.onNext(Pair(source, lastBlockInfo))
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
    }

    private suspend fun emitSyncing() {
        val syncing = adaptersMutex.withLock {
            adapters.any { it.value.transactionsState is AdapterState.Syncing }
        }
        syncingSubject.onNext(syncing)
    }

    override fun clear() {
        monitoringJob?.cancel()
        coroutineScope.cancel()
    }
}