package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction

import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1SendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Service
import io.horizontalsystems.ethereumkit.models.TransactionData

class WCSendEthereumTransactionRequestService(
    private val request: WC1SendEthereumTransactionRequest,
    private val baseService: WC1Service
) {
    private val transaction = request.transaction

    val transactionData = TransactionData(transaction.to, transaction.value, transaction.data)
    val gasPrice: Long? = transaction.gasPrice

    fun approve(transactionHash: ByteArray) {
        baseService.approveRequest(request.id, transactionHash)
    }

    fun reject() {
        baseService.rejectRequest(request.id)
    }

}
