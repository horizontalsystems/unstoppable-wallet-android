package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v1

import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WCRequestChain
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCRequestModule
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Service

class WCSendEthereumTransactionRequestService(
    private val requestId: Long,
    private val baseService: WC1Service
) : WCRequestModule.RequestAction {

    override val chain: WCRequestChain by lazy {
        val chainName = baseService.selectedBlockchain.name
        val address = baseService.evmKitWrapper?.evmKit?.receiveAddress?.eip55?.shorten() ?: ""
        WCRequestChain(chainName, address)
    }

    override fun approve(transactionHash: ByteArray) {
        baseService.approveRequest(requestId, transactionHash)
    }

    override fun reject() {
        baseService.rejectRequest(requestId)
    }

}
