package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate
import io.horizontalsystems.core.toHexString

class WCSendEthereumTransactionRequestViewModel: ViewModel() {

    fun approve(transactionHash: ByteArray) {
        WCDelegate.sessionRequestEvent?.let { sessionRequest ->
            WCDelegate.respondPendingRequest(sessionRequest.request.id, sessionRequest.topic, transactionHash.toHexString())
        }
    }

    fun reject() {
        WCDelegate.sessionRequestEvent?.let { sessionRequest ->
            WCDelegate.rejectRequest(sessionRequest.topic, sessionRequest.request.id)
        }
    }
}
