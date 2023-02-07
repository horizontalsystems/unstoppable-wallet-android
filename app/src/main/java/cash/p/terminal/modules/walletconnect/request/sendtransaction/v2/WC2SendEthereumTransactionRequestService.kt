package cash.p.terminal.modules.walletconnect.request.sendtransaction.v2

import cash.p.terminal.modules.walletconnect.request.sendtransaction.WCRequestModule
import cash.p.terminal.modules.walletconnect.version2.WC2SendEthereumTransactionRequest
import cash.p.terminal.modules.walletconnect.version2.WC2SessionManager
import io.horizontalsystems.core.toHexString

class WC2SendEthereumTransactionRequestService(
    private val requestData: WC2SessionManager.RequestData,
    private val sessionManager: WC2SessionManager,
) : WCRequestModule.RequestAction {

    val evmKitWrapper by lazy {
        requestData.evmKitWrapper
    }

    val transactionRequest by lazy {
        (requestData.pendingRequest as WC2SendEthereumTransactionRequest)
    }

    override fun approve(transactionHash: ByteArray) {
        sessionManager.service.respondPendingRequest(
            transactionRequest.id,
            transactionRequest.topic,
            transactionHash.toHexString()
        )
    }

    override fun reject() {
        sessionManager.service.rejectRequest(transactionRequest.topic, transactionRequest.id)
    }

}
