package io.horizontalsystems.bankwallet.modules.walletconnect.version2

import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WalletConnectTransaction
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.SignMessage
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.ethereumkit.spv.core.toLong
import java.math.BigInteger

interface WC2Request {
    val id: Long
    val topic: String
    val dAppName: String?
}

class WC2UnsupportedRequest(
    override val id: Long,
    override val topic: String,
    override val dAppName: String?,
) : WC2Request

class WC2SendEthereumTransactionRequest(
        override val id: Long,
        override val topic: String,
        override val dAppName: String?,
        val transaction: WalletConnectTransaction
) : WC2Request {

    constructor(
        id: Long,
        topic: String,
        dAppName: String?,
        transaction: WC2EthereumTransaction
    ) : this(id, topic, dAppName, convertTx(transaction))

    sealed class TransactionError : Exception() {
        class NoRecipient : TransactionError()
    }
}

class WC2SignMessageRequest(
        override val id: Long,
        override val topic: String,
        override val dAppName: String?,
        val rawData: String,
        val message: SignMessage
) : WC2Request

fun convertTx(transaction: WC2EthereumTransaction): WalletConnectTransaction {
    val to = transaction.to
    checkNotNull(to) {
        throw WC2SendEthereumTransactionRequest.TransactionError.NoRecipient()
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

data class WC2EthereumTransaction(
    val from: String,
    val to: String?,
    val nonce: String?,
    val gasPrice: String?,
    val gas: String?,
    val gasLimit: String?,
    val maxPriorityFeePerGas: String?,
    val maxFeePerGas: String?,
    val value: String?,
    val data: String
)
