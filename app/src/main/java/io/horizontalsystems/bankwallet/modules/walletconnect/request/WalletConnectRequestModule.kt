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
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

object WalletConnectRequestModule {

    class Factory(val request: WalletConnectSendEthereumTransactionRequest) : ViewModelProvider.Factory {
        private val ethereumKit by lazy { App.ethereumKitManager.evmKit!! }
        val transactionService by lazy {
            val feeRateProvider = FeeRateProviderFactory.provider(App.appConfigProvider.ethereumCoin) as EthereumFeeRateProvider
            EvmTransactionService(ethereumKit, feeRateProvider, 10)
        }
        private val coinService by lazy { CoinService(App.appConfigProvider.ethereumCoin, App.currencyManager, App.xRateManager) }
        private val stringProvider by lazy { StringProvider() }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return when (modelClass) {
                WalletConnectSendEthereumTransactionRequestViewModel::class.java -> {
                    val service = WalletConnectSendEthereumTransactionRequestService(request.transaction, transactionService, ethereumKit)
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
