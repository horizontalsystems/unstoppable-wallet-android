package io.horizontalsystems.bankwallet.modules.walletconnect.scanqr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService

object WalletConnectScanQrModule {

    class Factory(private val service: WalletConnectService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return WalletConnectScanQrViewModel(service) as T
        }
    }

}
