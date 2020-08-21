package io.horizontalsystems.bankwallet.modules.guides

import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.GuidesManager
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

class GuidesRepository(private val guidesManager: GuidesManager, private val connectivityManager: ConnectivityManager) {

    val guideCategories: Observable<DataState<Array<GuideCategory>>>
        get() = guideCategoriesSubject

    private val guideCategoriesSubject = BehaviorSubject.create<DataState<Array<GuideCategory>>>()
    private val disposables = CompositeDisposable()
    private val retryLimit = 3

    init {
        fetch()

        connectivityManager.networkAvailabilitySignal
                .subscribeOn(Schedulers.io())
                .subscribe {
                    if (connectivityManager.isConnected && guideCategoriesSubject.value is DataState.Error) {
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
        guideCategoriesSubject.onNext(DataState.Loading())

        guidesManager.getGuideCategories()
                //retry on error java.lang.AssertionError: No System TLS
                .retryWhen { errors ->
                    errors.zipWith(
                            Flowable.range(1, retryLimit + 1),
                            BiFunction<Throwable, Int, Int> { error: Throwable, retryCount: Int ->
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
                    guideCategoriesSubject.onNext(DataState.Success(it))
                }, {
                    guideCategoriesSubject.onNext(DataState.Error(it))
                })
                .let {
                    disposables.add(it)
                }
    }
}