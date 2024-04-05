package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class TransactionSyncStateRepository(
    private val adapterManager: TransactionAdapterManager
) : Clearable {
    private val adapters = mutableMapOf<TransactionSource, ITransactionsAdapter>()

    private val syncingSubject = PublishSubject.create<Boolean>()
    val syncingObservable: Observable<Boolean> get() = syncingSubject.distinctUntilChanged()

    private val lastBlockInfoSubject = PublishSubject.create<Pair<TransactionSource, LastBlockInfo>>()
    val lastBlockInfoObservable: Observable<Pair<TransactionSource, LastBlockInfo>> get() = lastBlockInfoSubject

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
                adapter.lastBlockUpdatedFlowable.asFlow().collect {
                    adapter.lastBlockInfo?.let { lastBlockInfo ->
                        lastBlockInfoSubject.onNext(Pair(source, lastBlockInfo))
                    }
                }
            }

            coroutineScope.launch {
                adapter.transactionsStateUpdatedFlowable.asFlow().collect {
                    emitSyncing()
                }
            }
        }
    }

    private fun emitSyncing() {
        val syncing = adapters.any {
            it.value.transactionsState is AdapterState.Syncing
        }
        syncingSubject.onNext(syncing)
    }

    override fun clear() {
        coroutineScope.cancel()
    }
}
