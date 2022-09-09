package io.horizontalsystems.bankwallet.modules.walletconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Service

object WalletConnectModule {

    class Factory(
        private val remotePeerId: String?,
        private val connectionLink: String?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = WC1Service(
                remotePeerId,
                connectionLink,
                App.wc1Manager,
                App.wc1SessionManager,
                App.wc1RequestManager,
                App.connectivityManager,
                App.evmBlockchainManager
            )

            return WalletConnectViewModel(service, listOf(service)) as T
        }
    }

}

enum class RequestType(val value: String) {
    PersonalSign("personal_sign"),
    EthSignTypedData("eth_signTypedData"),
    EthSendTransaction("eth_sendTransaction");

    companion object {
        private val map = values().associateBy(RequestType::value)

        fun fromString(value: String?): RequestType? = map[value]
    }
}
