package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList

class TransactionAdapterWrapper(
    private val transactionsAdapter: ITransactionsAdapter,
    private val transactionWallet: TransactionWallet,
    private var transactionType: FilterTransactionType
) : Clearable {
    private val updatedSubject = PublishSubject.create<Unit>()
    val updatedObservable: Observable<Unit> get() = updatedSubject

    private val transactionRecords = CopyOnWriteArrayList<TransactionRecord>()
    private var allLoaded = false
    private val disposables = CompositeDisposable()

    init {
        subscribeForUpdates()
    }

    fun reload() {
        unsubscribeFromUpdates()
        transactionRecords.clear()
        allLoaded = false
        subscribeForUpdates()
    }

    fun setTransactionType(transactionType: FilterTransactionType) {
        unsubscribeFromUpdates()

        this.transactionType = transactionType
        transactionRecords.clear()
        allLoaded = false
        subscribeForUpdates()
    }

    private fun subscribeForUpdates() {
        transactionsAdapter.getTransactionRecordsFlowable(transactionWallet.token, transactionType)
            .subscribeIO {
                transactionRecords.clear()
                allLoaded = false
                updatedSubject.onNext(Unit)
            }
            .let {
                disposables.add(it)
            }
    }

    private fun unsubscribeFromUpdates() {
        disposables.clear()
    }

    fun get(limit: Int): Single<List<TransactionRecord>> = when {
        transactionRecords.size >= limit || allLoaded -> Single.just(transactionRecords.take(limit))
        else -> {
            val numberOfRecordsToRequest = limit - transactionRecords.size
            transactionsAdapter
                .getTransactionsAsync(
                    transactionRecords.lastOrNull(),
                    transactionWallet.token,
                    numberOfRecordsToRequest,
                    transactionType
                )
                .map {
                    allLoaded = it.size < numberOfRecordsToRequest
                    transactionRecords.addAll(it)

                    transactionRecords
                }
        }
    }

    override fun clear() {
        unsubscribeFromUpdates()
    }
}
