package io.horizontalsystems.bankwallet.modules.settings.experimental.bitcoinhodling

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App

object BitcoinHodlingModule {

    interface IView {
        fun setLockTime(enabled: Boolean)
    }

    interface IViewDelegate {
        fun onLoad()
        fun onSwitchLockTime(enabled: Boolean)
    }

    interface IInteractor {
        var isLockTimeEnabled: Boolean
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = BitcoinHodlingView()
            val interactor = BitcoinHodlingInteractor(App.localStorage)
            val presenter = BitcoinHodlingPresenter(view, interactor)

            return presenter as T
        }
    }

    fun start(activity: FragmentActivity) {
        activity.supportFragmentManager.commit {
            add(R.id.fragmentContainerView, BitcoinHodlingFragment())
            addToBackStack(null)
        }
    }
}
