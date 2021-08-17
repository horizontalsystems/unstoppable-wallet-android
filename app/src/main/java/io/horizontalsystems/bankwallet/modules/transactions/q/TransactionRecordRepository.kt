package io.horizontalsystems.bankwallet.modules.transactions.q

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
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
) : Clearable, ITransactionRecordRepository {

    private var selectedWallet: TransactionWallet? = null

    private val itemsSubject = PublishSubject.create<List<TransactionRecord>>()
    override val itemsObservable: Observable<List<TransactionRecord>> get() = itemsSubject

    private val items = CopyOnWriteArrayList<TransactionRecord>()
    private val loading = AtomicBoolean(false)
    private var itemsUpdated = false
    private val adaptersMap = mutableMapOf<TransactionWallet, TransactionAdapterWrapper>()

    private val disposables = CompositeDisposable()

    private var walletsGroupedBySource: List<TransactionWallet> = listOf()

    override fun setWallets(transactionWallets: List<TransactionWallet>, walletsGroupedBySource: List<TransactionWallet>) {
        this.walletsGroupedBySource = walletsGroupedBySource

        transactionWallets.forEach { transactionWallet ->
            if (adaptersMap[transactionWallet] == null) {
                adapterManager.getAdapter(transactionWallet.source)?.let {
                    val transactionAdapterWrapperXxx = TransactionAdapterWrapper(it, transactionWallet)
                    adaptersMap[transactionWallet] = transactionAdapterWrapperXxx
                }
            }
        }

        walletsGroupedBySource.forEach { transactionWallet ->
            if (adaptersMap[transactionWallet] == null) {
                adapterManager.getAdapter(transactionWallet.source)?.let {
                    val transactionAdapterWrapperXxx = TransactionAdapterWrapper(it, transactionWallet)
                    adaptersMap[transactionWallet] = transactionAdapterWrapperXxx
                }
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
        val activeWallets = transactionWallet?.let { listOf(it) } ?: walletsGroupedBySource

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

    companion object {
        const val itemsPerPage = 20
    }

}