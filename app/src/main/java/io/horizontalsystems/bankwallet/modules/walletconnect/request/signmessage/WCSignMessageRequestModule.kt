package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v1.WC1SignMessageRequestService
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Service
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1SignMessageRequest

object WCSignMessageRequestModule {

    class Factory(
        private val signMessageRequest: WC1SignMessageRequest,
        private val baseService: WC1Service
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                WCSignMessageRequestViewModel::class.java -> {
                    val service = WC1SignMessageRequestService(
                        signMessageRequest,
                        baseService,
                        baseService.evmKitWrapper?.signer!!
                    )
                    WCSignMessageRequestViewModel(service) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    interface RequestAction{
        val message: SignMessage
        fun sign()
        fun reject()
    }

    sealed class SignMessage(val data: String) {
        class Message(data: String) : SignMessage(data)
        class PersonalMessage(data: String) : SignMessage(data)
        class TypedMessage(data: String, val domain: String) : SignMessage(data)
    }
}
