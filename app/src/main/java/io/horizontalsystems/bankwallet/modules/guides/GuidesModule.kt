package io.horizontalsystems.bankwallet.modules.guides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.GuidesManager
import io.horizontalsystems.bankwallet.entities.GuideCategory
import java.util.*

object GuidesModule {

    interface Interactor {
        fun fetchGuideCategories()
    }

    interface InteractorDelegate {
        fun didFetchGuideCategories(guideCategories: Array<GuideCategory>)
        fun onSelectFilter(filterId: String)

    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val interactor = GuidesInteractor(GuidesManager, App.networkManager)
            val presenter = GuidesViewModel(interactor)

            interactor.delegate = presenter

            return presenter as T
        }
    }
}
