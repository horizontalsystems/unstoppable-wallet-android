package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Service
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1SignMessageRequest

object WalletConnectSignMessageRequestModule {

    class Factory(
        private val signMessageRequest: WC1SignMessageRequest,
        private val baseService: WC1Service
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                WalletConnectSignMessageRequestViewModel::class.java -> {
                    val service = WalletConnectSignMessageRequestService(
                        signMessageRequest,
                        baseService,
                        baseService.evmKitWrapper?.signer!!
                    )
                    WalletConnectSignMessageRequestViewModel(service) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

}
