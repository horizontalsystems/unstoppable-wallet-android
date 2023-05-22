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
    protected val predefinedGasLimit: Long? = null
) {

    open fun estimatedGasDataAsync(gasPrice: GasPrice, transactionData: TransactionData, stubAmount: BigInteger? = null): Single<GasData> {
        if (predefinedGasLimit != null) {
            return Single.just(GasData(gasLimit = predefinedGasLimit, gasPrice = gasPrice))
        }

        val surchargeRequired = transactionData.input.isNotEmpty()

        val stubTransactionData = if (stubAmount != null)  {
            TransactionData(transactionData.to, BigInteger.ONE, transactionData.input)
        } else {
            transactionData
        }

        return evmKit.estimateGas(stubTransactionData, gasPrice)
                .map { estimatedGasLimit ->
                    val gasLimit = if (surchargeRequired) EvmFeeModule.surcharged(estimatedGasLimit) else estimatedGasLimit
                    GasData(
                        gasLimit = gasLimit,
                        estimatedGasLimit = estimatedGasLimit,
                        gasPrice = gasPrice
                    )
                }
    }

    companion object {
        fun instance(evmKit: EthereumKit, blockchainType: BlockchainType, gasLimit: Long? = null): EvmCommonGasDataService {
            val l1FeeContractAddress = blockchainType.l1GasFeeContractAddress

            return if (l1FeeContractAddress == null) {
                EvmCommonGasDataService(evmKit, gasLimit)
            } else {
                val l1FeeProvider = L1FeeProvider(evmKit, l1FeeContractAddress)
                EvmRollupGasDataService(evmKit, l1FeeProvider, gasLimit)
            }
        }
    }

}
