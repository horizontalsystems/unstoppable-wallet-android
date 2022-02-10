package io.horizontalsystems.bankwallet.modules.walletconnect.request

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
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WalletConnectSendEthereumTransactionRequestService
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WalletConnectSendEthereumTransactionRequestViewModel
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.CoinType
import java.math.BigInteger

object WalletConnectRequestModule {
    const val TYPED_MESSAGE = "typed_message"

    class Factory(
        private val request: WalletConnectSendEthereumTransactionRequest,
        private val baseService: WalletConnectService
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
        private val service by lazy { WalletConnectSendEthereumTransactionRequestService(request, baseService) }
        private val gasPriceService: IEvmGasPriceService by lazy {
            val gasPriceProvider = LegacyGasPriceProvider(evmKitWrapper.evmKit)
            when (evmKitWrapper.evmKit.networkType) {
                NetworkType.EthRopsten, NetworkType.EthKovan,
                NetworkType.EthGoerli, NetworkType.EthRinkeby,
                NetworkType.EthMainNet -> {
                    // TODO switch to EIP1559 GasPrice service after wallet connect v2 integration
                    LegacyGasPriceService(gasPriceProvider, service.gasPrice)
                }
                NetworkType.BscMainNet -> LegacyGasPriceService(gasPriceProvider, service.gasPrice)
            }
        }
        private val coinServiceFactory by lazy { EvmCoinServiceFactory(coin, App.marketKit, App.currencyManager) }
        private val feeService by lazy {
            EvmFeeService(evmKitWrapper.evmKit, gasPriceService, service.transactionData, 10)
        }
        private val cautionViewItemFactory by lazy { CautionViewItemFactory(coinServiceFactory.baseCoinService) }
        private val sendService by lazy {
            SendEvmTransactionService(
                SendEvmData(service.transactionData),
                evmKitWrapper,
                feeService,
                App.activateCoinManager
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                WalletConnectSendEthereumTransactionRequestViewModel::class.java -> {
                    WalletConnectSendEthereumTransactionRequestViewModel(service) as T
                }
                EvmFeeCellViewModel::class.java -> {
                    EvmFeeCellViewModel(feeService, gasPriceService, coinServiceFactory.baseCoinService) as T
                }
                SendEvmTransactionViewModel::class.java -> {
                    SendEvmTransactionViewModel(sendService, coinServiceFactory, cautionViewItemFactory) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
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
