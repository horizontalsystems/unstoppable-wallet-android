package io.horizontalsystems.bankwallet.modules.settings.appstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
            val appStatusService = AppStatusService(
                    App.systemInfoManager,
                    App.localStorage,
                    App.predefinedAccountTypeManager,
                    App.walletManager,
                    App.adapterManager,
                    App.coinManager,
                    App.ethereumKitManager,
                    App.binanceSmartChainKitManager,
                    App.binanceKitManager
            )
            val interactor = AppStatusInteractor(appStatusService, TextHelper)
            val presenter = AppStatusPresenter(view, interactor)

            return presenter as T
        }
    }
}
