package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.walletkit.modules.evmfee.GasPriceInfo
import java.math.BigInteger

data class SendTransactionSettingsEvm(
    val gasPriceInfo: GasPriceInfo?,
    val receiveAddress: Address,
) : SendTransactionSettings() {
    override val networkFee: SwapNetworkFee?
        get() = when (val gasPrice = gasPriceInfo?.gasPrice) {
            is GasPrice.Legacy -> SwapNetworkFee.Legacy(BigInteger.valueOf(gasPrice.legacyGasPrice))
            is GasPrice.Eip1559 -> SwapNetworkFee.Eip1559(
                BigInteger.valueOf(gasPrice.maxFeePerGas),
                BigInteger.valueOf(gasPrice.maxPriorityFeePerGas),
            )
            null -> null
        }
}
