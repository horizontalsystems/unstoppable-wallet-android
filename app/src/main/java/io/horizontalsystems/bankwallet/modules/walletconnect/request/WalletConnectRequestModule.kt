package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.core.providers.EthereumFeeRateProvider
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

object WalletConnectRequestModule {

    class Factory(
            private val request: WalletConnectSendEthereumTransactionRequest,
            private val baseService: WalletConnectService
    ) : ViewModelProvider.Factory {
        private val evmKit by lazy { baseService.evmKit!! }
        private val coin by lazy {
            when (evmKit.networkType) {
                NetworkType.EthMainNet,
                NetworkType.EthRopsten,
                NetworkType.EthKovan,
                NetworkType.EthRinkeby -> App.coinKit.getCoin(CoinType.Ethereum) ?: throw IllegalArgumentException()
                NetworkType.BscMainNet -> App.coinKit.getCoin(CoinType.BinanceSmartChain) ?: throw IllegalArgumentException()
            }
        }
        private val transactionService by lazy {
            val ethereumCoin = App.coinKit.getCoin(CoinType.Ethereum) ?: throw IllegalArgumentException()
            val feeRateProvider = FeeRateProviderFactory.provider(ethereumCoin) as EthereumFeeRateProvider
            EvmTransactionService(evmKit, feeRateProvider, 10)
        }
        private val coinService by lazy { CoinService(coin, App.currencyManager, App.xRateManager) }
        private val stringProvider by lazy { StringProvider() }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return when (modelClass) {
                WalletConnectSendEthereumTransactionRequestViewModel::class.java -> {
                    val service = WalletConnectSendEthereumTransactionRequestService(request, baseService, transactionService, evmKit)
                    WalletConnectSendEthereumTransactionRequestViewModel(service, coinService, stringProvider) as T
                }
                EthereumFeeViewModel::class.java -> {
                    EthereumFeeViewModel(transactionService, coinService, stringProvider) as T
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
