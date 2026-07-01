package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction

import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.ethereumkit.spv.core.toLong
import java.math.BigInteger

data class WCEthereumTransaction(
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
){
    fun getWCTransaction(): WalletConnectTransaction {
        val transaction = this
        val to = transaction.to
        checkNotNull(to) {
            throw TransactionError.NoRecipient()
        }

        return WalletConnectTransaction(
            from = Address(transaction.from),
            to = Address(to),
            nonce = transaction.nonce?.hexStringToByteArray()?.toLong(),
            gasPrice = transaction.gasPrice?.hexStringToByteArray()?.toLong(),
            gasLimit = (transaction.gas ?: transaction.gasLimit)?.hexStringToByteArray()?.toLong(),
            maxPriorityFeePerGas = transaction.maxPriorityFeePerGas?.hexStringToByteArray()
                ?.toLong(),
            maxFeePerGas = transaction.maxFeePerGas?.hexStringToByteArray()?.toLong(),
            value = transaction.value?.hexStringToByteArray()?.toBigInteger() ?: BigInteger.ZERO,
            data = transaction.data.hexStringToByteArray()
        )
    }

    sealed class TransactionError : Exception() {
        class NoRecipient : TransactionError()
    }
}