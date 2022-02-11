package io.horizontalsystems.bankwallet.modules.walletconnect

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable

class WalletConnectViewModel(
        val service: WalletConnectService,
        private val clearables: List<Clearable>
) : ViewModel() {

    var sharedSendEthereumTransactionRequest: WalletConnectSendEthereumTransactionRequest? = null
    var sharedSignMessageRequest: WalletConnectSignMessageRequest? = null

    override fun onCleared() {
        clearables.forEach {
            it.clear()
        }
        super.onCleared()
    }

}