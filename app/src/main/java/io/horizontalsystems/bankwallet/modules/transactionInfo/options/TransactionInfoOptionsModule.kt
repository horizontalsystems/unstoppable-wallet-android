package io.horizontalsystems.bankwallet.modules.transactionInfo.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ICustomRangedFeeProvider
import io.horizontalsystems.bankwallet.core.adapters.EvmTransactionsAdapter
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionFeeService
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoOption
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.core.EthereumKit
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

        private val evmKit by lazy {
            adapter.evmKit
        }

        private val baseCoin by lazy {
            when (evmKit.networkType) {
                EthereumKit.NetworkType.EthMainNet,
                EthereumKit.NetworkType.EthRopsten,
                EthereumKit.NetworkType.EthKovan,
                EthereumKit.NetworkType.EthGoerli,
                EthereumKit.NetworkType.EthRinkeby -> App.marketKit.platformCoin(CoinType.Ethereum)!!
                EthereumKit.NetworkType.BscMainNet -> App.marketKit.platformCoin(CoinType.BinanceSmartChain)!!
            }
        }

        private val fullTransaction by lazy {
            evmKit.getFullTransactions(listOf(transactionHash.hexStringToByteArray())).first()
        }
        private val transaction by lazy {
            fullTransaction.transaction
        }

        private val transactionService by lazy {
            val feeRateProvider = FeeRateProviderFactory.customRangedFeeProvider(
                coinType = baseCoin.coinType,
                customLowerBound = transaction.gasPrice,
                customUpperBound = null,
                multiply = 1.2
            ) as ICustomRangedFeeProvider
            EvmTransactionFeeService(evmKit, feeRateProvider)
        }

        private val coinServiceFactory by lazy {
            EvmCoinServiceFactory(
                baseCoin,
                App.marketKit,
                App.currencyManager,
            )
        }

        private val sendService by lazy {
            val transactionData = when (optionType) {
                TransactionInfoOption.Type.SpeedUp -> {
                    TransactionData(transaction.to!!, transaction.value, transaction.input, transaction.nonce)
                }
                TransactionInfoOption.Type.Cancel -> {
                    TransactionData(evmKit.receiveAddress, BigInteger.ZERO, byteArrayOf(), transaction.nonce)
                }
            }

            SendEvmTransactionService(SendEvmData(transactionData), evmKit, transactionService, App.activateCoinManager)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendEvmTransactionViewModel::class.java -> {
                    SendEvmTransactionViewModel(sendService, coinServiceFactory) as T
                }
                EthereumFeeViewModel::class.java -> {
                    EthereumFeeViewModel(transactionService, coinServiceFactory.baseCoinService) as T
                }
                TransactionSpeedUpCancelViewModel::class.java -> {
                    TransactionSpeedUpCancelViewModel(baseCoin, optionType, fullTransaction.receiptWithLogs == null) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

}
