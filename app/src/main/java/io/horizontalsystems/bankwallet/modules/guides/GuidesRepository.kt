package io.horizontalsystems.bankwallet.modules.guides

import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.GuidesManager
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class GuidesRepository(private val guidesManager: GuidesManager, private val connectivityManager: ConnectivityManager) {

    val guideCategories: Observable<DataState<Array<GuideCategory>>>
        get() = guideCategoriesSubject

    private val guideCategoriesSubject = BehaviorSubject.create<DataState<Array<GuideCategory>>>()
    private val disposables = CompositeDisposable()

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