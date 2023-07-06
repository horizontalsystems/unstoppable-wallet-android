package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v2

import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WCRequestChain
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCRequestModule
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager
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

    override val chain: WCRequestChain by lazy {
        val evmKit = evmKitWrapper.evmKit
        val chainName = evmKit.chain.name
        val address = evmKit.receiveAddress.eip55.shorten()
        WCRequestChain(chainName, address)
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
