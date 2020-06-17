package io.horizontalsystems.bankwallet.modules.guides

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.horizontalsystems.core.SingleLiveEvent

class GuidesViewModel(private val interactor: GuidesModule.Interactor) : GuidesModule.InteractorDelegate, ViewModel() {

    val openGuide = SingleLiveEvent<Guide>()
    val viewItemsLiveData = MutableLiveData<List<GuideViewItem>>()
    val spinner = MutableLiveData<Boolean>()
    val filters = MutableLiveData<List<String>>()

    private var guideCategories: Array<GuideCategory> = arrayOf()
    private var currentCategoryIndex = 0


    init {
        spinner.postValue(true)
        interactor.fetchGuideCategories()
    }

    fun onGuideClick(position: Int) {
        guideCategories[currentCategoryIndex].guides[position].let {
            openGuide.postValue(it)
        }
    }

    override fun didFetchGuideCategories(guideCategories: Array<GuideCategory>) {
        this.guideCategories = guideCategories

        filters.postValue(guideCategories.map { it.title })
        spinner.postValue(false)

        syncViewItems()
    }

    private fun syncViewItems() {
        val viewItems = guideCategories[currentCategoryIndex].guides.map { guide ->
                GuideViewItem(guide.title, guide.updatedAt, guide.imageUrl)
        }
        viewItemsLiveData.postValue(viewItems)
    }
}
