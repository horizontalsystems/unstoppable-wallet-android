package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService

object WalletConnectRequestModule {

    class Factory(private val service: WalletConnectService, val requestId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return WalletConnectRequestViewModel(service, requestId) as T
        }
    }

}
