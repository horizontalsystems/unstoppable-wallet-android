package io.horizontalsystems.bankwallet.modules.settings.faq

import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.FaqManager
import io.horizontalsystems.bankwallet.entities.FaqMap
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

class FaqRepository(private val faqManager: FaqManager, private val connectivityManager: ConnectivityManager) {

    val faqList: Observable<DataState<Array<FaqMap>>>
        get() = faqListSubject

    private val faqListSubject = BehaviorSubject.create<DataState<Array<FaqMap>>>()
    private val disposables = CompositeDisposable()
    private val retryLimit = 3

    init {
        fetch()

        connectivityManager.networkAvailabilitySignal
            .subscribeOn(Schedulers.io())
            .subscribe {
                if (connectivityManager.isConnected && faqListSubject.value is DataState.Error) {
                    fetch()
                }
            }
            .let {
                disposables.add(it)
            }
    }

    fun clear() {
        disposables.clear()
    }

    private fun fetch() {
        faqListSubject.onNext(DataState.Loading())

        faqManager.getFaqList()
            .retryWhen { errors -> // retry on error java.lang.AssertionError: No System TLS
                errors.zipWith(
                    Flowable.range(1, retryLimit + 1),
                    { error: Throwable, retryCount: Int ->
                        if (retryCount < retryLimit && (error is AssertionError)) {
                            retryCount
                        } else {
                            throw error
                        }
                    }
                ).flatMap {
                    Flowable.timer(1, TimeUnit.SECONDS)
                }
            }
            .subscribeOn(Schedulers.io())
            .subscribe({
                faqListSubject.onNext(DataState.Success(it.toTypedArray()))
            }, {
                faqListSubject.onNext(DataState.Error(it))
            })
            .let {
                disposables.add(it)
            }
    }
}
