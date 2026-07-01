package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.GasPrice
import java.math.BigInteger

data class WalletConnectTransaction(
    val from: Address,
    val to: Address,
    val nonce: Long?,
    val gasPrice: Long?,
    val gasLimit: Long?,
    val maxPriorityFeePerGas: Long?,
    val maxFeePerGas: Long?,
    val value: BigInteger,
    val data: ByteArray
) {
    fun getGasPriceObj() = when {
        maxFeePerGas != null && maxPriorityFeePerGas != null -> {
            GasPrice.Eip1559(maxFeePerGas, maxPriorityFeePerGas)
        }
        this.gasPrice != null -> {
            GasPrice.Legacy(this.gasPrice)
        }
        else -> null
    }
}
