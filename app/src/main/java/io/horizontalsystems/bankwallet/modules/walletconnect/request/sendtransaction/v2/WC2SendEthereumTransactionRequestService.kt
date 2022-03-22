package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v2

import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCRequestModule
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager
import io.horizontalsystems.core.toHexString

class WC2SendEthereumTransactionRequestService(
    private val requestId: Long,
    private val sessionManager: WC2SessionManager,
) : WCRequestModule.RequestAction {

    private val requestData by lazy {
        sessionManager.pendingRequestDataToOpen[requestId]!!
    }

    val evmKitWrapper by lazy {
        requestData.evmKitWrapper
    }

    val transactionRequest by lazy {
        (requestData.pendingRequest as WC2SendEthereumTransactionRequest)
    }

    override fun stop() {
        sessionManager.pendingRequestDataToOpen.remove(requestId)
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
