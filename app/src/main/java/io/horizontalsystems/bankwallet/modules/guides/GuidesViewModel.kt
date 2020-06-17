package io.horizontalsystems.bankwallet.modules.guides

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.GuidesManager
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class GuidesViewModel(private val guidesManager: GuidesManager) : ViewModel() {

    val openGuide = SingleLiveEvent<Guide>()
    val guidesLiveData = MutableLiveData<List<Guide>>()
    val loading = MutableLiveData<Boolean>()
    val filters = MutableLiveData<List<String>>()

    private var guideCategories: Array<GuideCategory> = arrayOf()
    private var currentCategoryIndex = 0

    private var disposable: Disposable? = null

    init {
        loading.postValue(true)
        disposable = guidesManager.getGuideCategories()
                .subscribeOn(Schedulers.io())
                .subscribe { categories, _ ->
                    didFetchGuideCategories(categories)
                }
    }

    fun onSelectFilter(filterId: String) {
        currentCategoryIndex = guideCategories.indexOfFirst {
            it.title == filterId
        }

        syncViewItems()
    }

    fun onGuideClick(guide: Guide) {
        openGuide.postValue(guide)
    }

    private fun didFetchGuideCategories(guideCategories: Array<GuideCategory>) {
        this.guideCategories = guideCategories

        filters.postValue(guideCategories.map { it.title })
        loading.postValue(false)

        syncViewItems()
    }

    private fun syncViewItems() {
        guidesLiveData.postValue(guideCategories[currentCategoryIndex].guides)
    }

    override fun onCleared() {
        disposable?.dispose()
    }
}
