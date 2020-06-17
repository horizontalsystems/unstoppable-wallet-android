package io.horizontalsystems.bankwallet.modules.guides

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.horizontalsystems.core.SingleLiveEvent

class GuidesViewModel(private val interactor: GuidesModule.Interactor) : GuidesModule.InteractorDelegate, ViewModel() {

    val openGuide = SingleLiveEvent<Guide>()
    val guidesLiveData = MutableLiveData<List<Guide>>()
    val loading = MutableLiveData<Boolean>()
    val filters = MutableLiveData<List<String>>()

    private var guideCategories: Array<GuideCategory> = arrayOf()
    private var currentCategoryIndex = 0


    init {
        loading.postValue(true)
        interactor.fetchGuideCategories()
    }

    fun onGuideClick(guide: Guide) {
        openGuide.postValue(guide)
    }

    override fun didFetchGuideCategories(guideCategories: Array<GuideCategory>) {
        this.guideCategories = guideCategories

        filters.postValue(guideCategories.map { it.title })
        loading.postValue(false)

        syncViewItems()
    }

    override fun onSelectFilter(filterId: String) {
        currentCategoryIndex = guideCategories.indexOfFirst {
            it.title == filterId
        }

        syncViewItems()
    }

    private fun syncViewItems() {
        guidesLiveData.postValue(guideCategories[currentCategoryIndex].guides)
    }
}
