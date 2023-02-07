package cash.p.terminal.modules.transactionInfo.options

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.adapters.EvmTransactionsAdapter
import cash.p.terminal.core.ethereum.CautionViewItemFactory
import cash.p.terminal.core.ethereum.EvmCoinServiceFactory
import cash.p.terminal.modules.evmfee.EvmCommonGasDataService
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.evmfee.EvmFeeService
import cash.p.terminal.modules.evmfee.IEvmGasPriceService
import cash.p.terminal.modules.evmfee.eip1559.Eip1559GasPriceService
import cash.p.terminal.modules.evmfee.legacy.LegacyGasPriceService
import cash.p.terminal.modules.send.evm.SendEvmData
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionService
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewModel
import cash.p.terminal.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

object TransactionInfoOptionsModule {

    @Parcelize
    enum class Type : Parcelable {
        SpeedUp, Cancel
    }

    class Factory(
        private val optionType: Type,
        private val transactionHash: String,
        private val source: TransactionSource
    ) : ViewModelProvider.Factory {

        private val adapter by lazy {
            App.transactionAdapterManager.getAdapter(source) as EvmTransactionsAdapter
        }

        private val evmKitWrapper by lazy {
            adapter.evmKitWrapper
        }

        private val baseToken by lazy {
            val blockchainType = when (evmKitWrapper.evmKit.chain) {
                Chain.BinanceSmartChain -> BlockchainType.BinanceSmartChain
                Chain.Polygon -> BlockchainType.Polygon
                Chain.Avalanche -> BlockchainType.Avalanche
                Chain.Optimism -> BlockchainType.Optimism
                Chain.ArbitrumOne -> BlockchainType.ArbitrumOne
                Chain.Gnosis -> BlockchainType.Gnosis
                Chain.EthereumGoerli -> BlockchainType.EthereumGoerli
                else -> BlockchainType.Ethereum
            }
            App.evmBlockchainManager.getBaseToken(blockchainType)!!
        }

        private val fullTransaction by lazy {
            evmKitWrapper.evmKit.getFullTransactions(listOf(transactionHash.hexStringToByteArray())).first()
        }
        private val transaction by lazy {
            fullTransaction.transaction
        }

        private val gasPriceService: IEvmGasPriceService by lazy {
            val evmKit = evmKitWrapper.evmKit
            if (evmKit.chain.isEIP1559Supported) {
                val gasPriceProvider = Eip1559GasPriceProvider(evmKit)
                val minGasPrice = transaction.maxFeePerGas?.let { maxFeePerGas ->
                    transaction.maxPriorityFeePerGas?.let { maxPriorityFeePerGas ->
                        GasPrice.Eip1559(maxFeePerGas, maxPriorityFeePerGas)
                    }
                }
                Eip1559GasPriceService(gasPriceProvider, evmKit, minGasPrice = minGasPrice)
            } else {
                val gasPriceProvider = LegacyGasPriceProvider(evmKit)
                LegacyGasPriceService(gasPriceProvider, transaction.gasPrice)
            }
        }

        private val transactionData by lazy {
            when (optionType) {
                Type.SpeedUp -> {
                    TransactionData(transaction.to!!, transaction.value!!, transaction.input!!, transaction.nonce)
                }
                Type.Cancel -> {
                    TransactionData(
                        evmKitWrapper.evmKit.receiveAddress,
                        BigInteger.ZERO,
                        byteArrayOf(),
                        transaction.nonce
                    )
                }
            }
        }
        private val transactionService by lazy {
            val gasDataService = EvmCommonGasDataService.instance(evmKitWrapper.evmKit, evmKitWrapper.blockchainType, gasLimit = transaction.gasLimit)
            EvmFeeService(evmKitWrapper.evmKit, gasPriceService, gasDataService, transactionData)
        }

        private val coinServiceFactory by lazy {
            EvmCoinServiceFactory(
                baseToken,
                App.marketKit,
                App.currencyManager,
                App.evmTestnetManager,
                App.coinManager
            )
        }
        private val cautionViewItemFactory by lazy { CautionViewItemFactory(coinServiceFactory.baseCoinService) }

        private val sendService by lazy {
            SendEvmTransactionService(
                SendEvmData(transactionData),
                evmKitWrapper,
                transactionService,
                App.evmLabelManager
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendEvmTransactionViewModel::class.java -> {
                    SendEvmTransactionViewModel(sendService, coinServiceFactory, cautionViewItemFactory, App.evmLabelManager) as T
                }
                EvmFeeCellViewModel::class.java -> {
                    EvmFeeCellViewModel(transactionService, gasPriceService, coinServiceFactory.baseCoinService) as T
                }
                TransactionSpeedUpCancelViewModel::class.java -> {
                    TransactionSpeedUpCancelViewModel(
                        baseToken,
                        optionType,
                        fullTransaction.transaction.blockNumber == null
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

}
