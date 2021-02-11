package io.horizontalsystems.bankwallet.modules.walletconnect

import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WalletConnectTransaction
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.ethereumkit.spv.core.toLong
import java.math.BigInteger
import kotlin.Exception

interface WalletConnectRequest {
    val id: Long

    fun convertResult(result: Any): String?
}

class WalletConnectSendEthereumTransactionRequest(override val id: Long, val transaction: WalletConnectTransaction) : WalletConnectRequest {

    constructor(id: Long, transaction: WCEthereumTransaction) : this(id, convertTx(transaction))

    override fun convertResult(result: Any): String? {
        return (result as? ByteArray)?.toHexString()
    }

    sealed class TransactionError : Exception() {
        class NoRecipient : TransactionError()
    }
}


fun convertTx(transaction: WCEthereumTransaction): WalletConnectTransaction {
    val to = transaction.to
    checkNotNull(to) {
        throw WalletConnectSendEthereumTransactionRequest.TransactionError.NoRecipient()
    }

    return WalletConnectTransaction(
            from = Address(transaction.from),
            to = Address(to),
            nonce = transaction.nonce?.hexStringToByteArray()?.toLong(),
            gasPrice = transaction.gasPrice?.hexStringToByteArray()?.toLong(),
            gasLimit = (transaction.gas ?: transaction.gasLimit)?.hexStringToByteArray()?.toLong(),
            value = transaction.value?.hexStringToByteArray()?.toBigInteger() ?: BigInteger.ZERO,
            data = transaction.data.hexStringToByteArray()
    )
}
