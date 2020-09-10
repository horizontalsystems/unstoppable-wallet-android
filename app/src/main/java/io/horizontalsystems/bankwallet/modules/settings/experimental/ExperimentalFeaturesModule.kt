package io.horizontalsystems.bankwallet.modules.settings.experimental

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R

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

    fun start(activity: FragmentActivity) {
        activity.supportFragmentManager.commit {
            add(R.id.fragmentContainerView, ExperimentalFeaturesFragment())
            addToBackStack(null)
        }
    }
}
