package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WCRequestChain
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v2.WC2SignMessageRequestService
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager

object WCSignMessageRequestModule {

    class FactoryWC2(private val requestData: WC2SessionManager.RequestData) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                WCSignMessageRequestViewModel::class.java -> {
                    val service = WC2SignMessageRequestService(
                        requestData,
                        App.wc2SessionManager
                    )
                    WCSignMessageRequestViewModel(service) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    interface RequestAction {
        val dAppName: String?
        val message: SignMessage
        val chain: WCRequestChain
        val isLegacySignRequest: Boolean
        fun sign()
        fun reject()
    }

}

sealed class SignMessage(val data: String) {
    class Message(data: String, val showLegacySignWarning: Boolean = false) : SignMessage(data)
    class PersonalMessage(data: String) : SignMessage(data)
    class TypedMessage(data: String, val domain: String? = null) : SignMessage(data)
}
