package io.horizontalsystems.bankwallet.modules.guides

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.core.SingleLiveEvent

class GuidesViewModel(private val interactor: GuidesModule.Interactor) : GuidesModule.InteractorDelegate, ViewModel() {

    val openGuide = SingleLiveEvent<Guide>()
    val viewItemsLiveData = MutableLiveData<List<GuideViewItem>>()

    private val guides = interactor.guides

    init {
        val viewItems = guides.map {
            GuideViewItem(it.title, it.date, it.imageUrl)
        }

        viewItemsLiveData.postValue(viewItems)
    }

    fun onGuideClick(position: Int) {
        openGuide.postValue(guides[position])
    }

}
