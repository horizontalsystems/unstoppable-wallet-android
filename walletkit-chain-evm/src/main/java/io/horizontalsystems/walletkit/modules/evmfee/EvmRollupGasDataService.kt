package io.horizontalsystems.walletkit.modules.evmfee

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToBigIntegerOrNull
import io.horizontalsystems.ethereumkit.core.rollup.L1FeeProvider
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import java.math.BigInteger

class EvmRollupGasDataService(
    evmKit: EthereumKit,
    private val l1FeeProvider: L1FeeProvider,
    predefinedGasLimit: Long? = null
) : EvmCommonGasDataService(evmKit, predefinedGasLimit) {

    override suspend fun estimatedGasData(gasPrice: GasPrice, transactionData: TransactionData, stubAmount: BigInteger?): GasData =
        if (predefinedGasLimit != null) {
            val l1Fee = l1GasFee(transactionData, gasPrice, predefinedGasLimit)
            RollupGasData(gasLimit = predefinedGasLimit, gasPrice = gasPrice, l1Fee = l1Fee)
        } else {
            val gasData = super.estimatedGasData(gasPrice, transactionData, stubAmount)
            val gasLimit = gasData.gasLimit
            val stubTransactionData = if (stubAmount != null) {
                TransactionData(transactionData.to, maxBytes(transactionData.value), transactionData.input)
            } else {
                transactionData
            }

            val l1Fee = l1GasFee(stubTransactionData, gasPrice, gasLimit)
            RollupGasData(gasLimit = gasLimit, gasPrice = gasPrice, l1Fee = l1Fee)
        }

    private fun maxBytes(value: BigInteger): BigInteger {
        val hexString = value.toString(16)
        val maximumHexValue = "F".repeat(hexString.length)

        return maximumHexValue.hexStringToBigIntegerOrNull() ?: value
    }

    private suspend fun l1GasFee(transactionData: TransactionData, gasPrice: GasPrice, gasLimit: Long): BigInteger =
        l1FeeProvider.getL1Fee(gasPrice, gasLimit, transactionData.to, transactionData.value, transactionData.input)

}
