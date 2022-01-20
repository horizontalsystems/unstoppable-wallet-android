package io.horizontalsystems.bankwallet.modules.walletconnect.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object WalletConnectListModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = WalletConnectListService(App.walletConnectSessionManager)

            return WalletConnectListViewModel(service) as T
        }
    }
}
