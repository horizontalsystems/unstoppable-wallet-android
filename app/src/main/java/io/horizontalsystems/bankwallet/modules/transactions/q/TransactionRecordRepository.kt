package io.horizontalsystems.bankwallet.modules.transactions.q

import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionWallet
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class TransactionRecordRepository(
    private val adapterManager: TransactionAdapterManager,
) : ITransactionRecordRepository {

    private var selectedWallet: TransactionWallet? = null

    private val itemsSubject = PublishSubject.create<List<TransactionRecord>>()
    override val itemsObservable: Observable<List<TransactionRecord>> get() = itemsSubject

    private val items = CopyOnWriteArrayList<TransactionRecord>()
    private val loading = AtomicBoolean(false)
    private var itemsUpdated = false
    private val adaptersMap = mutableMapOf<TransactionWallet, TransactionAdapterWrapper>()

    private val disposables = CompositeDisposable()
    private val disposablesTxsUpdates = CompositeDisposable()

    private var walletsGroupedBySource: List<TransactionWallet> = listOf()

    override fun setWallets(transactionWallets: List<TransactionWallet>, walletsGroupedBySource: List<TransactionWallet>) {
        this.walletsGroupedBySource = walletsGroupedBySource

        val currentAdapters = adaptersMap.toMutableMap()
        adaptersMap.clear()
        disposablesTxsUpdates.clear()

        (transactionWallets + walletsGroupedBySource).distinct().forEach { transactionWallet ->
            var adapter = currentAdapters.remove(transactionWallet)
            if (adapter == null) {
                adapterManager.getAdapter(transactionWallet.source)?.let {
                    adapter = TransactionAdapterWrapper(it, transactionWallet)
                }
            }

            adapter?.let {
                adaptersMap[transactionWallet] = it
            }
        }

        adaptersMap.forEach { (transactionWallet, adapterWrapper) ->
            adapterWrapper.updatedObservable
                .subscribeIO {
                    handleUpdatedRecords(transactionWallet, it)
                }
                .let {
                    disposablesTxsUpdates.add(it)
                }
        }

        currentAdapters.clear()

        selectedWallet?.let {
            if (!adaptersMap.containsKey(it)) {
                selectedWallet = null
            }
        }

        items.clear()
        itemsUpdated = true
        adaptersMap.forEach { t, u ->
            u.markUsed(null)
        }
        loadNext()
    }

    override fun setSelectedWallet(transactionWallet: TransactionWallet?) {
        selectedWallet = transactionWallet

        items.clear()
        itemsUpdated = true
        adaptersMap.forEach { t, u ->
            u.markUsed(null)
        }
        loadNext()
    }

    override fun loadNext() {
        if (loading.get()) return
        loading.set(true)

        val activeWallets = selectedWallet?.let { listOf(it) } ?: walletsGroupedBySource

        val map: List<Single<List<Pair<TransactionWallet, TransactionRecord>>>> = activeWallets.mapNotNull { transactionWallet ->
            adaptersMap[transactionWallet]?.let { transactionAdapterWrapperXxx ->
                transactionAdapterWrapperXxx
                    .getNext(itemsPerPage)
                    .map { transactionRecords: List<TransactionRecord> ->
                        transactionRecords.map {
                            Pair(transactionWallet, it)
                        }
                    }
            }
        }

        Single
            .zip(map) {
                it as Array<List<Pair<TransactionWallet, TransactionRecord>>>
                it.toList().flatten()
            }
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .doFinally {
                loading.set(false)
            }
            .subscribe { records ->
                handleRecords(records)
            }
            .let {
                disposables.add(it)
            }
    }

    override fun clear() {
        disposables.clear()
        disposablesTxsUpdates.clear()
    }

    private fun handleRecords(records: List<Pair<TransactionWallet, TransactionRecord>>) {
        records
            .sortedByDescending { it.second }
            .take(itemsPerPage)
            .forEach {
                adaptersMap[it.first]?.markUsed(it.second)

                items.add(it.second)
                itemsUpdated = true
            }

        if (itemsUpdated) {
            itemsSubject.onNext(items)
            itemsUpdated = false
        }
    }

    @Synchronized
    private fun handleUpdatedRecords(transactionWallet: TransactionWallet, list: List<TransactionRecord>) {
        val transactionAdapterWrapper = adaptersMap[transactionWallet]
        list.forEach { updatedRecord ->
            val indexOfUpdated = items.indexOf(updatedRecord)
            val indexToInsert = items.indexOfFirst { it < updatedRecord }

            when {
                // record should be updated
                indexOfUpdated != -1 -> {
                    items[indexOfUpdated] = updatedRecord
                    itemsUpdated = true
                    transactionAdapterWrapper?.markUpdated(updatedRecord)
                }
                // record should be inserted
                indexToInsert != -1 -> {
                    items.add(indexToInsert, updatedRecord)
                    itemsUpdated = true
                    transactionAdapterWrapper?.markInserted(updatedRecord)
                }
                // if current items empty insert a record
                items.isEmpty() -> {
                    items.add(updatedRecord)
                    itemsUpdated = true
                    transactionAdapterWrapper?.markInserted(updatedRecord)
                }
                else -> {
                    transactionAdapterWrapper?.markIgnored(updatedRecord)
                }
            }
        }

        if (itemsUpdated) {
            itemsSubject.onNext(items)
            itemsUpdated = false
        }
    }


    companion object {
        const val itemsPerPage = 20
    }

}