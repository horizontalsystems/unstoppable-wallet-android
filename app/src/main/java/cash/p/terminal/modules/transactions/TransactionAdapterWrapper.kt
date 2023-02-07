package cash.p.terminal.modules.transactions

import cash.p.terminal.core.Clearable
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.transactionrecords.TransactionRecord
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
