package io.horizontalsystems.bankwallet.modules.guides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

object GuidesModule {

    interface View {
        fun open(item: GuideItem)

    }

    interface ViewDelegate {
        fun onLoad()
        fun onGuideClick(item: GuideItem)
    }

    interface Interactor {

    }

    interface InteractorDelegate {

    }

    interface Router {

    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = GuidesView()
            val interactor = GuidesInteractor()
            val presenter = GuidesPresenter(view, interactor)

            interactor.delegate = presenter

            return presenter as T
        }
    }
}
