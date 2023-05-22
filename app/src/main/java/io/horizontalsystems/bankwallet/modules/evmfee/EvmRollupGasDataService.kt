package io.horizontalsystems.bankwallet.modules.evmfee

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToBigIntegerOrNull
import io.horizontalsystems.ethereumkit.core.rollup.L1FeeProvider
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.Single
import java.math.BigInteger

class EvmRollupGasDataService(
    evmKit: EthereumKit,
    private val l1FeeProvider: L1FeeProvider,
    predefinedGasLimit: Long? = null
) : EvmCommonGasDataService(evmKit, predefinedGasLimit) {

    override fun estimatedGasDataAsync(gasPrice: GasPrice, transactionData: TransactionData, stubAmount: BigInteger?): Single<GasData> =
        if (predefinedGasLimit != null) {
            l1GasFee(transactionData, gasPrice, predefinedGasLimit).map { l1Fee ->
                RollupGasData(gasLimit = predefinedGasLimit, gasPrice = gasPrice, l1Fee = l1Fee)
            }
        } else {
            super.estimatedGasDataAsync(gasPrice, transactionData, stubAmount).flatMap { gasData ->
                val gasLimit = gasData.gasLimit
                val stubTransactionData = if (stubAmount != null) {
                    TransactionData(transactionData.to, maxBytes(transactionData.value), transactionData.input)
                } else {
                    transactionData
                }

                l1GasFee(stubTransactionData, gasPrice, gasLimit).map { l1Fee ->
                    RollupGasData(gasLimit = gasLimit, gasPrice = gasPrice, l1Fee = l1Fee)
                }
            }
        }

    private fun maxBytes(value: BigInteger): BigInteger {
        val hexString = value.toString(16)
        val maximumHexValue = "F".repeat(hexString.length)

        return maximumHexValue.hexStringToBigIntegerOrNull() ?: value
    }

    private fun l1GasFee(transactionData: TransactionData, gasPrice: GasPrice, gasLimit: Long): Single<BigInteger> =
        l1FeeProvider.getL1Fee(gasPrice, gasLimit, transactionData.to, transactionData.value, transactionData.input)

}
