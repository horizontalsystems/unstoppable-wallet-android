package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v1

import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCRequestModule
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Service

class WCSendEthereumTransactionRequestService(
    private val requestId: Long,
    private val baseService: WC1Service
) : WCRequestModule.RequestAction {

    override fun approve(transactionHash: ByteArray) {
        baseService.approveRequest(requestId, transactionHash)
    }

    override fun reject() {
        baseService.rejectRequest(requestId)
    }

}
