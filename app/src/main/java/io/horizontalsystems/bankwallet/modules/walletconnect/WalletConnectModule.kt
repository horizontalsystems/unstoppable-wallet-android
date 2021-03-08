package io.horizontalsystems.bankwallet.modules.walletconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object WalletConnectModule {

    class Factory(private val remotePeerId: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = WalletConnectService(remotePeerId, App.walletConnectManager, App.walletConnectSessionManager, App.connectivityManager)

            return WalletConnectViewModel(service, listOf(service)) as T
        }
    }

}
