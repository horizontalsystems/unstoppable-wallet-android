package cash.p.terminal.modules.walletconnect.request.sendtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.ethereum.CautionViewItemFactory
import cash.p.terminal.core.ethereum.EvmCoinServiceFactory
import cash.p.terminal.modules.evmfee.EvmCommonGasDataService
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.evmfee.EvmFeeService
import cash.p.terminal.modules.evmfee.IEvmGasPriceService
import cash.p.terminal.modules.evmfee.eip1559.Eip1559GasPriceService
import cash.p.terminal.modules.evmfee.legacy.LegacyGasPriceService
import cash.p.terminal.modules.send.evm.SendEvmData
import cash.p.terminal.modules.send.evm.SendEvmData.AdditionalInfo
import cash.p.terminal.modules.send.evm.SendEvmData.WalletConnectInfo
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceService
import cash.p.terminal.modules.send.evm.settings.SendEvmSettingsService
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionService
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewModel
import cash.p.terminal.modules.walletconnect.request.sendtransaction.v1.WCSendEthereumTransactionRequestService
import cash.p.terminal.modules.walletconnect.request.sendtransaction.v2.WC2SendEthereumTransactionRequestService
import cash.p.terminal.modules.walletconnect.version1.WC1SendEthereumTransactionRequest
import cash.p.terminal.modules.walletconnect.version1.WC1Service
import cash.p.terminal.modules.walletconnect.version2.WC2SessionManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigInteger

object WCRequestModule {

    class Factory(
        private val request: WC1SendEthereumTransactionRequest,
        private val baseService: WC1Service,
        dAppName: String?
    ) : ViewModelProvider.Factory {
        private val evmKitWrapper by lazy { baseService.evmKitWrapper!! }
        private val blockchainType = when (evmKitWrapper.evmKit.chain) {
            Chain.BinanceSmartChain -> BlockchainType.BinanceSmartChain
            Chain.Polygon -> BlockchainType.Polygon
            Chain.Avalanche -> BlockchainType.Avalanche
            Chain.Optimism -> BlockchainType.Optimism
            Chain.ArbitrumOne -> BlockchainType.ArbitrumOne
            Chain.Gnosis -> BlockchainType.Gnosis
            else -> BlockchainType.Ethereum
        }
        private val token by lazy {
            getToken(blockchainType)
        }
        private val transaction = request.transaction
        private val transactionData =
            TransactionData(transaction.to, transaction.value, transaction.data)

        private val gasPrice by lazy { getGasPrice(transaction) }

        private val service by lazy {
            WCSendEthereumTransactionRequestService(request.id, baseService)
        }
        private val gasPriceService: IEvmGasPriceService by lazy {
            getGasPriceService(gasPrice, evmKitWrapper.evmKit)
        }

        private val coinServiceFactory by lazy {
            EvmCoinServiceFactory(
                token,
                App.marketKit,
                App.currencyManager,
                App.coinManager
            )
        }
        private val feeService by lazy {
            val gasDataService = EvmCommonGasDataService(evmKitWrapper.evmKit, 10)
            EvmFeeService(evmKitWrapper.evmKit, gasPriceService, gasDataService, transactionData)
        }
        private val cautionViewItemFactory by lazy { CautionViewItemFactory(coinServiceFactory.baseCoinService) }
        private val additionalInfo = AdditionalInfo.WalletConnectRequest(WalletConnectInfo(dAppName))
        private val settingsService by lazy { SendEvmSettingsService(feeService, SendEvmNonceService(evmKitWrapper.evmKit, transaction.nonce)) }
        private val sendService by lazy {
            SendEvmTransactionService(
                SendEvmData(transactionData, additionalInfo),
                evmKitWrapper,
                settingsService,
                App.evmLabelManager
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
                        cautionViewItemFactory,
                        blockchainType = blockchainType,
                        contactsRepo = App.contactsRepository
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    class FactoryV2(private val requestData: WC2SessionManager.RequestData) : ViewModelProvider.Factory {
        private val service by lazy {
            WC2SendEthereumTransactionRequestService(requestData, App.wc2SessionManager)
        }
        private val blockchainType = when (service.evmKitWrapper.evmKit.chain) {
            Chain.BinanceSmartChain -> BlockchainType.BinanceSmartChain
            Chain.Polygon -> BlockchainType.Polygon
            Chain.Avalanche -> BlockchainType.Avalanche
            Chain.Optimism -> BlockchainType.Optimism
            Chain.ArbitrumOne -> BlockchainType.ArbitrumOne
            Chain.Gnosis -> BlockchainType.Gnosis
            else -> BlockchainType.Ethereum
        }
        private val token by lazy {
            getToken(blockchainType)
        }
        private val transaction = service.transactionRequest.transaction
        private val transactionData =
            TransactionData(transaction.to, transaction.value, transaction.data)

        private val gasPrice by lazy { getGasPrice(transaction) }

        private val gasPriceService by lazy {
            getGasPriceService(gasPrice, service.evmKitWrapper.evmKit)
        }

        private val coinServiceFactory by lazy {
            EvmCoinServiceFactory(
                token,
                App.marketKit,
                App.currencyManager,
                App.coinManager
            )
        }
        private val feeService by lazy {
            val evmKitWrapper = service.evmKitWrapper
            val gasDataService = EvmCommonGasDataService.instance(evmKitWrapper.evmKit, evmKitWrapper.blockchainType, 10)
            EvmFeeService(evmKitWrapper.evmKit, gasPriceService, gasDataService, transactionData)
        }
        private val cautionViewItemFactory by lazy { CautionViewItemFactory(coinServiceFactory.baseCoinService) }
        private val additionalInfo = AdditionalInfo.WalletConnectRequest(WalletConnectInfo(service.transactionRequest.dAppName))
        private val settingsService by lazy { SendEvmSettingsService(feeService, SendEvmNonceService(service.evmKitWrapper.evmKit, transaction.nonce)) }

        private val sendService by lazy {
            SendEvmTransactionService(
                SendEvmData(transactionData, additionalInfo),
                service.evmKitWrapper,
                settingsService,
                App.evmLabelManager
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
                        cautionViewItemFactory,
                        blockchainType = blockchainType,
                        contactsRepo = App.contactsRepository
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    private fun getToken(blockchainType: BlockchainType): Token {
        return App.evmBlockchainManager.getBaseToken(blockchainType)!!
    }

    private fun getGasPrice(transaction: WalletConnectTransaction): GasPrice? = when {
        transaction.maxFeePerGas != null && transaction.maxPriorityFeePerGas != null -> {
            GasPrice.Eip1559(transaction.maxFeePerGas, transaction.maxPriorityFeePerGas)
        }
        else -> {
            transaction.gasPrice?.let { GasPrice.Legacy(it) }
        }
    }

    private fun getGasPriceService(gasPrice: GasPrice?, evmKit: EthereumKit): IEvmGasPriceService {
        return when {
            gasPrice is GasPrice.Legacy || gasPrice == null && !evmKit.chain.isEIP1559Supported -> {
                val gasPriceProvider = LegacyGasPriceProvider(evmKit)
                LegacyGasPriceService(
                    gasPriceProvider,
                    initialGasPrice = (gasPrice as? GasPrice.Legacy)?.legacyGasPrice
                )
            }
            else -> {
                val gasPriceProvider = Eip1559GasPriceProvider(evmKit)
                Eip1559GasPriceService(
                    gasPriceProvider,
                    evmKit,
                    initialGasPrice = gasPrice as? GasPrice.Eip1559
                )
            }
        }
    }

    interface RequestAction {
        fun approve(transactionHash: ByteArray)
        fun reject()
    }

}

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
)