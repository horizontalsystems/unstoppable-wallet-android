package cash.p.terminal.modules.settings.appstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App

object AppStatusModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appStatusService = AppStatusService(
                    App.systemInfoManager,
                    App.localStorage,
                    App.accountManager,
                    App.walletManager,
                    App.adapterManager,
                    App.marketKit,
            )
            return AppStatusViewModel(appStatusService) as T
        }
    }
}
