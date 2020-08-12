package io.horizontalsystems.bankwallet.modules.guides

import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.GuidesManager
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class GuidesService(private val guidesManager: GuidesManager, private val connectivityManager: ConnectivityManager) {

    sealed class GuideCategoryResult {
        object Loading : GuideCategoryResult()
        class Success(val guideCategories: Array<GuideCategory>) : GuideCategoryResult()
        class Error(val throwable: Throwable) : GuideCategoryResult()
    }

    val guideCategories : Observable<GuideCategoryResult>
        get() = guideCategoriesSubject

    private val guideCategoriesSubject = BehaviorSubject.create<GuideCategoryResult>()
    private var disposables = CompositeDisposable()

    init {
        fetch()

        connectivityManager.networkAvailabilitySignal
                .subscribe {
                    if (connectivityManager.isConnected && guideCategoriesSubject.value is GuideCategoryResult.Error ) {
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
        guideCategoriesSubject.onNext(GuideCategoryResult.Loading)

        GuidesManager.getGuideCategories()
                .subscribe({
                    guideCategoriesSubject.onNext(GuideCategoryResult.Success(it))
                }, {
                    guideCategoriesSubject.onNext(GuideCategoryResult.Error(it))
                })
                .let {
                    disposables.add(it)
                }
    }



}