package cash.p.terminal.modules.walletconnect

import androidx.lifecycle.ViewModel
import cash.p.terminal.core.Clearable
import cash.p.terminal.modules.walletconnect.version1.WC1SendEthereumTransactionRequest
import cash.p.terminal.modules.walletconnect.version1.WC1Service
import cash.p.terminal.modules.walletconnect.version1.WC1SignMessageRequest

class WalletConnectViewModel(
    val service: WC1Service,
    private val clearables: List<Clearable>
) : ViewModel() {

    var sharedSendEthereumTransactionRequest: WC1SendEthereumTransactionRequest? = null
    var sharedSignMessageRequest: WC1SignMessageRequest? = null
    var dAppName: String? = null

    override fun onCleared() {
        clearables.forEach {
            it.clear()
        }
        super.onCleared()
    }

}