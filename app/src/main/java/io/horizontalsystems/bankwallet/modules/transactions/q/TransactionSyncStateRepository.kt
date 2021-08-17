package io.horizontalsystems.bankwallet.modules.transactions.q

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class TransactionSyncStateRepository(private val adapterManager: TransactionAdapterManager) {
    private val adapters = mutableMapOf<TransactionSource, ITransactionsAdapter>()

    private val syncingSubject = PublishSubject.create<Boolean>()
    val syncingObservable: Observable<Boolean> get() = syncingSubject.distinctUntilChanged()

    private val lastBlockInfoSubject = PublishSubject.create<Pair<TransactionSource, LastBlockInfo>>()
    val lastBlockInfoObservable: Observable<Pair<TransactionSource, LastBlockInfo>> get() = lastBlockInfoSubject

    private val disposables = CompositeDisposable()

    fun getLastBlockInfo(source: TransactionSource): LastBlockInfo? = adapters[source]?.lastBlockInfo

    fun setTransactionSources(sources: List<TransactionSource>) {
        disposables.clear()
        adapters.clear()

        sources.forEach { source ->
            adapterManager.getAdapter(source)?.let { adapter ->
                adapters[source] = adapter

                adapter.lastBlockUpdatedFlowable
                    .subscribeIO {
                        adapter.lastBlockInfo?.let { lastBlockInfo ->
                            lastBlockInfoSubject.onNext(Pair(source, lastBlockInfo))
                        }
                    }
                    .let {
                        disposables.add(it)
                    }

                adapter.transactionsStateUpdatedFlowable
                    .subscribeIO {
                        val syncing = adapters.any {
                            it.value.transactionsState is AdapterState.Syncing
                        }
                        syncingSubject.onNext(syncing)
                    }
                    .let {
                        disposables.add(it)
                    }
            }
        }
    }

}
