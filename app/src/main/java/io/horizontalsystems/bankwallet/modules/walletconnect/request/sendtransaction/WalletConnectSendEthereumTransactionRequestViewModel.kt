package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction

import androidx.lifecycle.ViewModel

class WalletConnectSendEthereumTransactionRequestViewModel(
        private val service: WalletConnectSendEthereumTransactionRequestService
) : ViewModel() {

    fun approve(transactionHash: ByteArray) {
        service.approve(transactionHash)
    }

    fun reject() {
        service.reject()
    }

}
