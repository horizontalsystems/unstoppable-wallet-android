package io.horizontalsystems.bankwallet.modules.settings.experimental

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

object ExperimentalFeaturesModule {

    interface IViewDelegate {
        fun didTapBitcoinHodling()
    }

    interface IRouter {
        fun showBitcoinHodling()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val router = ExperimentalFeaturesRouter()
            val presenter = ExperimentalFeaturesPresenter(router)

            return presenter as T
        }
    }
}
