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
    private val transactionWallet: TransactionWallet
) : Clearable {
    private val updatedSubject = PublishSubject.create<Unit>()
    val updatedObservable: Observable<Unit> get() = updatedSubject

    private val transactionRecords = CopyOnWriteArrayList<TransactionRecord>()

    private val disposables = CompositeDisposable()

    init {
        transactionsAdapter.getTransactionRecordsFlowable(transactionWallet.coin)
            .subscribeIO {
                transactionRecords.clear()
                updatedSubject.onNext(Unit)
            }
            .let {
                disposables.add(it)
            }
    }

    fun get(limit: Int): Single<List<TransactionRecord>> = when {
        transactionRecords.size >= limit -> Single.just(transactionRecords.take(limit))
        else -> {
            val numberOfRecordsToRequest = limit - transactionRecords.size
            transactionsAdapter
                .getTransactionsAsync(
                    transactionRecords.lastOrNull(),
                    transactionWallet.coin,
                    numberOfRecordsToRequest
                )
                .map {
                    transactionRecords.addAll(it)

                    transactionRecords
                }
        }
    }

    override fun clear() {
        disposables.clear()
    }
}
