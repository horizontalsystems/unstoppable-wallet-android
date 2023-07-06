package cash.p.terminal.modules.walletconnect.request.sendtransaction.v1

import cash.p.terminal.core.shorten
import cash.p.terminal.modules.walletconnect.request.WCRequestChain
import cash.p.terminal.modules.walletconnect.request.sendtransaction.WCRequestModule
import cash.p.terminal.modules.walletconnect.version1.WC1Service

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
