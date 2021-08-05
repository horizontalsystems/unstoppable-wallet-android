package io.horizontalsystems.bankwallet.modules.transactions.q

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class Transactions2Service(private val transactionRecordRepository: TransactionRecordRepository) : Clearable {

    private val itemsSubject = BehaviorSubject.create<List<TransactionItem>>()
    val itemsObservable: Observable<List<TransactionItem>> get() = itemsSubject

    val syncingObservable: Observable<Boolean> = Observable.just(true)

    private val disposables = CompositeDisposable()

    init {
        transactionRecordRepository.itemsObservable
            .subscribeIO {
                itemsSubject.onNext(it.map {
                    TransactionItem(it, null, null)
                })
            }
            .let {
                disposables.add(it)
            }
    }

    override fun clear() {
        disposables.clear()
    }
}

