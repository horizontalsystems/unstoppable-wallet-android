package io.horizontalsystems.bankwallet.modules.guides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.managers.GuidesManager
import io.horizontalsystems.bankwallet.entities.Guide
import java.util.*

object GuidesModule {

    interface Interactor {
        val guides: List<Guide>
    }

    interface InteractorDelegate {

    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val interactor = GuidesInteractor(GuidesManager)
            val presenter = GuidesViewModel(interactor)

            interactor.delegate = presenter

            return presenter as T
        }
    }
}

data class GuideViewItem(val title: String,
                         val date: Date,
                         val imageUrl: String?)

