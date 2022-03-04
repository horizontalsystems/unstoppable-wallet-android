package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
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
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v1.WCSendEthereumTransactionRequestService
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.v2.WC2SendEthereumTransactionRequestService
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1SendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Service
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.CoinType
import java.math.BigInteger

object WCRequestModule {

    class Factory(
        private val request: WC1SendEthereumTransactionRequest,
        private val baseService: WC1Service
    ) : ViewModelProvider.Factory {
        private val evmKitWrapper by lazy { baseService.evmKitWrapper!! }
        private val coin by lazy { platformCoin(evmKitWrapper.evmKit.chain) }
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
            EvmCoinServiceFactory(coin, App.marketKit, App.currencyManager)
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

    class FactoryV2(private val requestId: Long) : ViewModelProvider.Factory {
        private val service by lazy {
            WC2SendEthereumTransactionRequestService(requestId, App.wc2SessionManager)
        }
        private val coin by lazy { platformCoin(service.evmKitWrapper.evmKit.chain) }
        private val transaction = service.transactionRequest.transaction
        private val transactionData =
            TransactionData(transaction.to, transaction.value, transaction.data)

        private val gasPrice by lazy { getGasPrice(transaction) }

        private val gasPriceService by lazy {
            getGasPriceService(gasPrice, service.evmKitWrapper.evmKit)
        }

        private val coinServiceFactory by lazy {
            EvmCoinServiceFactory(coin, App.marketKit, App.currencyManager)
        }
        private val feeService by lazy {
            EvmFeeService(
                service.evmKitWrapper.evmKit,
                gasPriceService,
                transactionData,
                10
            )
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

    private fun platformCoin(chain: Chain) =
        when (chain) {
            Chain.BinanceSmartChain -> App.coinManager.getPlatformCoin(CoinType.BinanceSmartChain)!!
            else -> App.coinManager.getPlatformCoin(CoinType.Ethereum)!!
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
        return when (gasPrice) {
            is GasPrice.Eip1559 -> {
                val gasPriceProvider = Eip1559GasPriceProvider(evmKit)
                Eip1559GasPriceService(gasPriceProvider, evmKit, initialGasPrice = gasPrice)
            }
            else -> {
                val gasPriceProvider = LegacyGasPriceProvider(evmKit)
                LegacyGasPriceService(
                    gasPriceProvider,
                    initialGasPrice = (gasPrice as? GasPrice.Legacy)?.legacyGasPrice
                )
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
    val maxPriorityFeePerGas: Long?,
    val maxFeePerGas: Long?,
    val value: BigInteger,
    val data: ByteArray
)
