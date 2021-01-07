package io.horizontalsystems.bankwallet.modules.market.categories

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable

class MarketCategoriesViewModel(private val service: MarketCategoriesService) : ViewModel() {

    val categories by service::categories
    var currentCategory by service::currentCategory
    val categoryLiveData = MutableLiveData(currentCategory)

    private val disposable = CompositeDisposable()

    init {
        service.currentCategoryChangedObservable
                .subscribe {
                    categoryLiveData.postValue(service.currentCategory)
                }
                .let {
                    disposable.add(it)
                }
    }

}
