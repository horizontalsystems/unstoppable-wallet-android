package io.horizontalsystems.bankwallet.modules.guides

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.reactivex.disposables.CompositeDisposable

class GuidesViewModel(val service: GuidesService) : ViewModel() {

    val guides = MutableLiveData<List<Guide>>()
    val loading = MutableLiveData<Boolean>(false)
    val filters = MutableLiveData<List<String>>()

    private var guideCategories: Array<GuideCategory> = arrayOf()
    private var currentCategoryIndex = 0
    private var disposables = CompositeDisposable()

    init {
        service.guideCategories
                .subscribe {
                    loading.postValue(it is GuidesService.GuideCategoryResult.Loading)

                    when (it) {
                        is GuidesService.GuideCategoryResult.Success -> {
                            didFetchGuideCategories(it.guideCategories)
                        }
                        is GuidesService.GuideCategoryResult.Error -> {

                        }
                    }
                }
                .let {
                    disposables.add(it)
                }
    }

    fun onSelectFilter(filterId: String) {
        currentCategoryIndex = guideCategories.indexOfFirst {
            it.title == filterId
        }

        syncViewItems()
    }

    override fun onCleared() {
        disposables.dispose()

        service.clear()
    }

    private fun didFetchGuideCategories(guideCategories: Array<GuideCategory>) {
        this.guideCategories = guideCategories

        filters.postValue(guideCategories.map { it.title })

        syncViewItems()
    }

    private fun syncViewItems() {
        guides.postValue(guideCategories[currentCategoryIndex].guides)
    }
}
