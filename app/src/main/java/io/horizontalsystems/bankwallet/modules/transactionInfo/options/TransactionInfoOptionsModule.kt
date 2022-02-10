package io.horizontalsystems.bankwallet.modules.transactionInfo.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.EvmTransactionsAdapter
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItemFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeService
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmGasPriceService
import io.horizontalsystems.bankwallet.modules.evmfee.eip1559.Eip1559GasPriceService
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyGasPriceService
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoOption
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.CoinType
import java.math.BigInteger

object TransactionInfoOptionsModule {

    class Factory(
        private val optionType: TransactionInfoOption.Type,
        private val transactionHash: String,
        private val source: TransactionSource
    ) : ViewModelProvider.Factory {

        private val adapter by lazy {
            App.transactionAdapterManager.getAdapter(source) as EvmTransactionsAdapter
        }

        private val evmKitWrapper by lazy {
            adapter.evmKitWrapper
        }

        private val baseCoin by lazy {
            when (evmKitWrapper.evmKit.networkType) {
                NetworkType.EthMainNet,
                NetworkType.EthRopsten,
                NetworkType.EthKovan,
                NetworkType.EthGoerli,
                NetworkType.EthRinkeby -> App.marketKit.platformCoin(CoinType.Ethereum)!!
                NetworkType.BscMainNet -> App.marketKit.platformCoin(CoinType.BinanceSmartChain)!!
            }
        }

        private val fullTransaction by lazy {
            evmKitWrapper.evmKit.getFullTransactions(listOf(transactionHash.hexStringToByteArray())).first()
        }
        private val transaction by lazy {
            fullTransaction.transaction
        }

        private val gasPriceService: IEvmGasPriceService by lazy {
            val evmKit = evmKitWrapper.evmKit
            when (evmKit.networkType) {
                NetworkType.EthRopsten, NetworkType.EthKovan,
                NetworkType.EthGoerli, NetworkType.EthRinkeby,
                NetworkType.EthMainNet -> {
                    val gasPriceProvider = Eip1559GasPriceProvider(evmKit)
                    Eip1559GasPriceService(gasPriceProvider, evmKit, transaction.maxPriorityFeePerGas)
                }
                NetworkType.BscMainNet -> {
                    val gasPriceProvider = LegacyGasPriceProvider(evmKit)
                    LegacyGasPriceService(gasPriceProvider, transaction.gasPrice)
                }
            }
        }

        private val transactionData by lazy {
            when (optionType) {
                TransactionInfoOption.Type.SpeedUp -> {
                    TransactionData(transaction.to!!, transaction.value, transaction.input, transaction.nonce)
                }
                TransactionInfoOption.Type.Cancel -> {
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
            EvmFeeService(evmKitWrapper.evmKit, gasPriceService, transactionData)
        }

        private val coinServiceFactory by lazy { EvmCoinServiceFactory(baseCoin, App.marketKit, App.currencyManager) }
        private val cautionViewItemFactory by lazy { CautionViewItemFactory(coinServiceFactory.baseCoinService) }

        private val sendService by lazy {
            SendEvmTransactionService(
                SendEvmData(transactionData),
                evmKitWrapper,
                transactionService,
                App.activateCoinManager
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendEvmTransactionViewModel::class.java -> {
                    SendEvmTransactionViewModel(sendService, coinServiceFactory, cautionViewItemFactory) as T
                }
                EvmFeeCellViewModel::class.java -> {
                    EvmFeeCellViewModel(transactionService, gasPriceService, coinServiceFactory.baseCoinService) as T
                }
                TransactionSpeedUpCancelViewModel::class.java -> {
                    TransactionSpeedUpCancelViewModel(
                        baseCoin,
                        optionType,
                        fullTransaction.receiptWithLogs == null
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

}
