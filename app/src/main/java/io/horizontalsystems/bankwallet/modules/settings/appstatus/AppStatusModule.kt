package io.horizontalsystems.bankwallet.modules.settings.appstatus

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper

object AppStatusModule {

    interface IView {
        fun setAppStatus(status: Map<String, Any>)
        fun showCopied()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun didTapCopy(text: String)
    }

    interface IInteractor {
        val status: Map<String, Any>

        fun copyToClipboard(text: String)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = AppStatusView()
            val interactor = AppStatusInteractor(App.appStatusManager, TextHelper)
            val presenter = AppStatusPresenter(view, interactor)

            return presenter as T
        }
    }

    fun start(activity: FragmentActivity) {
        activity.supportFragmentManager.commit {
            add(R.id.fragmentContainerView, AppStatusFragment())
            addToBackStack(null)
        }
    }
}
