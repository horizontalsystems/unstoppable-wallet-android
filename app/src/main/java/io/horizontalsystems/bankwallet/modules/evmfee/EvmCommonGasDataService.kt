package io.horizontalsystems.bankwallet.modules.evmfee

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.rollup.L1FeeProvider
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.Single
import java.math.BigInteger

open class EvmCommonGasDataService(
        private val evmKit: EthereumKit,
        private val gasLimitSurchargePercent: Int = 0,
        private val gasLimit: Long? = null
) {

    open fun predefinedGasDataAsync(gasPrice: GasPrice, transactionData: TransactionData): Single<GasData>? {
        if (gasLimit == null) {
            return null
        }

        return Single.just(GasData(gasLimit, gasPrice))
    }

    open fun estimatedGasDataAsync(gasPrice: GasPrice, transactionData: TransactionData, stubAmount: BigInteger?): Single<GasData> {
        val stubTransactionData = if (stubAmount != null)  {
            TransactionData(transactionData.to, BigInteger.ONE, transactionData.input)
        } else {
            transactionData
        }

        return evmKit.estimateGas(stubTransactionData, gasPrice)
                .map { estimatedGasLimit ->
                    val gasLimit = getSurchargedGasLimit(estimatedGasLimit)
                    GasData(gasLimit, gasPrice)
                }

    }

    private fun getSurchargedGasLimit(estimatedGasLimit: Long): Long {
        return (estimatedGasLimit + estimatedGasLimit / 100.0 * gasLimitSurchargePercent).toLong()
    }

    companion object {
        fun instance(evmKit: EthereumKit, blockchainType: BlockchainType, gasLimitSurchargePercent: Int = 0, gasLimit: Long? = null): EvmCommonGasDataService {
            val l1FeeContractAddress = blockchainType.l1GasFeeContractAddress

            return if (l1FeeContractAddress == null) {
                EvmCommonGasDataService(evmKit, gasLimitSurchargePercent, gasLimit)
            } else {
                val l1FeeProvider = L1FeeProvider(evmKit, l1FeeContractAddress)
                EvmRollupGasDataService(evmKit, l1FeeProvider, gasLimitSurchargePercent, gasLimit)
            }
        }
    }

}
