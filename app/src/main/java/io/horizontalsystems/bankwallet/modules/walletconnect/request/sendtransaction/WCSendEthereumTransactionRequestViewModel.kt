package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction

import androidx.lifecycle.ViewModel

class WCSendEthereumTransactionRequestViewModel(
        private val service: WCRequestModule.RequestAction
) : ViewModel() {

    fun approve(transactionHash: ByteArray) {
        service.approve(transactionHash)
    }

    fun reject() {
        service.reject()
    }
}
