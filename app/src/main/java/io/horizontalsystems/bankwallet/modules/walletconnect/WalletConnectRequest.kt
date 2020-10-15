package io.horizontalsystems.bankwallet.modules.walletconnect

import android.os.Parcelable
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WalletConnectTransaction
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.ethereumkit.spv.core.toLong
import kotlinx.android.parcel.Parcelize
import kotlin.Exception

interface WalletConnectRequest {
    val id: Long
}

class WalletConnectSendEthereumTransactionRequest(override val id: Long, val transaction: WalletConnectTransaction) : WalletConnectRequest {

    constructor(id: Long, transaction: WCEthereumTransaction, x: Boolean = false) : this(id, convertTx(transaction))

    sealed class TransactionError : Exception() {
        class UnsupportedRequestType : TransactionError()
        class InvalidRecipient : TransactionError()
        class InvalidGasLimit : TransactionError()
        class InvalidValue : TransactionError()
        class InvalidData : TransactionError()
    }
}


fun convertTx(transaction: WCEthereumTransaction): WalletConnectTransaction {
    val to = transaction.to
    checkNotNull(to) {
        throw WalletConnectSendEthereumTransactionRequest.TransactionError.InvalidRecipient()
    }

    val gasLimitString = transaction.gas ?: transaction.gasLimit

    checkNotNull(gasLimitString) {
        throw WalletConnectSendEthereumTransactionRequest.TransactionError.InvalidGasLimit()
    }

    val gasLimit = gasLimitString.hexStringToByteArray().toLong()

    val value = transaction.value?.hexStringToByteArray()?.toBigInteger()

    checkNotNull(value) {
        throw WalletConnectSendEthereumTransactionRequest.TransactionError.InvalidValue()
    }

    val data = transaction.data.hexStringToByteArray()

    val walletConnectTransaction = WalletConnectTransaction(
            from = Address(transaction.from),
            to = Address(to),
            nonce = transaction.nonce?.hexStringToByteArray()?.toLong(),
            gasPrice = transaction.gasPrice?.hexStringToByteArray()?.toLong(),
            gasLimit = gasLimit,
            value = value,
            data = data
    )
    return walletConnectTransaction
}
