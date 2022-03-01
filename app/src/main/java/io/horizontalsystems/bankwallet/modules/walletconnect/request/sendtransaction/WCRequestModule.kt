package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItemFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeService
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmGasPriceService
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyGasPriceService
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v1.WCSendEthereumTransactionRequestService
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v2.WC2SendEthereumTransactionRequestService
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1SendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Service
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.CoinType
import java.math.BigInteger

object WCRequestModule {

    class Factory(
        private val request: WC1SendEthereumTransactionRequest,
        private val baseService: WC1Service
    ) : ViewModelProvider.Factory {
        private val evmKitWrapper by lazy { baseService.evmKitWrapper!! }
        private val coin by lazy {
            when (evmKitWrapper.evmKit.networkType) {
                NetworkType.EthRopsten, NetworkType.EthKovan,
                NetworkType.EthGoerli, NetworkType.EthRinkeby,
                NetworkType.EthMainNet -> App.coinManager.getPlatformCoin(CoinType.Ethereum)!!
                NetworkType.BscMainNet -> App.coinManager.getPlatformCoin(CoinType.BinanceSmartChain)!!
            }
        }
        private val transaction = request.transaction
        private val transactionData =
            TransactionData(transaction.to, transaction.value, transaction.data)
        private val gasPrice: Long? = transaction.gasPrice

        private val service by lazy {
            WCSendEthereumTransactionRequestService(request.id, baseService)
        }
        private val gasPriceService: IEvmGasPriceService by lazy {
            val gasPriceProvider = LegacyGasPriceProvider(evmKitWrapper.evmKit)
            when (evmKitWrapper.evmKit.networkType) {
                NetworkType.EthRopsten, NetworkType.EthKovan,
                NetworkType.EthGoerli, NetworkType.EthRinkeby,
                NetworkType.EthMainNet -> {
                    // TODO switch to EIP1559 GasPrice service after wallet connect v2 integration
                    LegacyGasPriceService(gasPriceProvider, gasPrice)
                }
                NetworkType.BscMainNet -> LegacyGasPriceService(gasPriceProvider, gasPrice)
            }
        }
        private val coinServiceFactory by lazy {
            EvmCoinServiceFactory(
                coin,
                App.marketKit,
                App.currencyManager
            )
        }
        private val feeService by lazy {
            EvmFeeService(evmKitWrapper.evmKit, gasPriceService, transactionData, 10)
        }
        private val cautionViewItemFactory by lazy { CautionViewItemFactory(coinServiceFactory.baseCoinService) }
        private val sendService by lazy {
            SendEvmTransactionService(
                SendEvmData(transactionData),
                evmKitWrapper,
                feeService,
                App.activateCoinManager
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                WCSendEthereumTransactionRequestViewModel::class.java -> {
                    WCSendEthereumTransactionRequestViewModel(service) as T
                }
                EvmFeeCellViewModel::class.java -> {
                    EvmFeeCellViewModel(
                        feeService,
                        gasPriceService,
                        coinServiceFactory.baseCoinService
                    ) as T
                }
                SendEvmTransactionViewModel::class.java -> {
                    SendEvmTransactionViewModel(
                        sendService,
                        coinServiceFactory,
                        cautionViewItemFactory
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    class FactoryV2(
        private val requestId: Long,
    ) : ViewModelProvider.Factory {
        private val service by lazy {
            WC2SendEthereumTransactionRequestService(requestId, App.wc2SessionManager)
        }
        private val coin by lazy {
            when (service.evmKitWrapper.evmKit.networkType) {
                NetworkType.EthRopsten, NetworkType.EthKovan,
                NetworkType.EthGoerli, NetworkType.EthRinkeby,
                NetworkType.EthMainNet -> App.coinManager.getPlatformCoin(CoinType.Ethereum)!!
                NetworkType.BscMainNet -> App.coinManager.getPlatformCoin(CoinType.BinanceSmartChain)!!
            }
        }
        private val transaction = service.transactionRequest.transaction
        private val transactionData =
            TransactionData(transaction.to, transaction.value, transaction.data)
        private val gasPrice: Long? = transaction.gasPrice

        private val gasPriceService: IEvmGasPriceService by lazy {
            val gasPriceProvider = LegacyGasPriceProvider(service.evmKitWrapper.evmKit)
            when (service.evmKitWrapper.evmKit.networkType) {
                NetworkType.EthRopsten, NetworkType.EthKovan,
                NetworkType.EthGoerli, NetworkType.EthRinkeby,
                NetworkType.EthMainNet -> {
                    // TODO switch to EIP1559 GasPrice service after wallet connect v2 integration
                    LegacyGasPriceService(gasPriceProvider, gasPrice)
                }
                NetworkType.BscMainNet -> LegacyGasPriceService(gasPriceProvider, gasPrice)
            }
        }
        private val coinServiceFactory by lazy {
            EvmCoinServiceFactory(
                coin,
                App.marketKit,
                App.currencyManager
            )
        }
        private val feeService by lazy {
            EvmFeeService(service.evmKitWrapper.evmKit, gasPriceService, transactionData, 10)
        }
        private val cautionViewItemFactory by lazy { CautionViewItemFactory(coinServiceFactory.baseCoinService) }
        private val sendService by lazy {
            SendEvmTransactionService(
                SendEvmData(transactionData),
                service.evmKitWrapper,
                feeService,
                App.activateCoinManager
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                WCSendEthereumTransactionRequestViewModel::class.java -> {
                    WCSendEthereumTransactionRequestViewModel(service) as T
                }
                EvmFeeCellViewModel::class.java -> {
                    EvmFeeCellViewModel(
                        feeService,
                        gasPriceService,
                        coinServiceFactory.baseCoinService
                    ) as T
                }
                SendEvmTransactionViewModel::class.java -> {
                    SendEvmTransactionViewModel(
                        sendService,
                        coinServiceFactory,
                        cautionViewItemFactory
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    interface RequestAction {
        fun approve(transactionHash: ByteArray)
        fun reject()
        fun stop()
    }

}

data class WalletConnectTransaction(
    val from: Address,
    val to: Address,
    val nonce: Long?,
    val gasPrice: Long?,
    val gasLimit: Long?,
    val value: BigInteger,
    val data: ByteArray
)
