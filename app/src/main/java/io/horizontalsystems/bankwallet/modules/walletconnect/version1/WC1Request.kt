package io.horizontalsystems.bankwallet.modules.walletconnect.version1

import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WalletConnectTransaction
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.ethereumkit.spv.core.toLong
import java.math.BigInteger

interface WC1Request {
    val id: Long

    fun convertResult(result: Any): String?
}

class WC1SendEthereumTransactionRequest(
        override val id: Long,
        val transaction: WalletConnectTransaction
) : WC1Request {

    constructor(id: Long, transaction: WCEthereumTransaction) : this(id, convertTx(transaction))

    override fun convertResult(result: Any): String? {
        return (result as? ByteArray)?.toHexString()
    }

    sealed class TransactionError : Exception() {
        class NoRecipient : TransactionError()
    }
}

class WC1SignMessageRequest(
        override val id: Long,
        val message: WCEthereumSignMessage
) : WC1Request {

    override fun convertResult(result: Any): String? {
        return (result as? ByteArray)?.toHexString()
    }
}

fun convertTx(transaction: WCEthereumTransaction): WalletConnectTransaction {
    val to = transaction.to
    checkNotNull(to) {
        throw WC1SendEthereumTransactionRequest.TransactionError.NoRecipient()
    }

    return WalletConnectTransaction(
            from = Address(transaction.from),
            to = Address(to),
            nonce = transaction.nonce?.hexStringToByteArray()?.toLong(),
            gasPrice = transaction.gasPrice?.hexStringToByteArray()?.toLong(),
            gasLimit = (transaction.gas ?: transaction.gasLimit)?.hexStringToByteArray()?.toLong(),
            maxPriorityFeePerGas= transaction.maxPriorityFeePerGas?.hexStringToByteArray()?.toLong(),
            maxFeePerGas= transaction.maxFeePerGas?.hexStringToByteArray()?.toLong(),
            value = transaction.value?.hexStringToByteArray()?.toBigInteger() ?: BigInteger.ZERO,
            data = transaction.data.hexStringToByteArray()
    )
}
