package io.horizontalsystems.bankwallet.modules.swap_new

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.ethereum.EthereumTransactionService
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.core.providers.EthereumFeeRateProvider
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.provider.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.view.SwapItemFormatter
import io.horizontalsystems.bankwallet.modules.swap_new.providers.SwapCoinProvider
import io.horizontalsystems.bankwallet.modules.swap_new.repositories.UniswapRepository
import io.horizontalsystems.bankwallet.modules.swap_new.viewmodels.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap_new.viewmodels.SwapFromCoinCardViewModel
import io.horizontalsystems.bankwallet.modules.swap_new.viewmodels.SwapToCoinCardViewModel
import io.horizontalsystems.bankwallet.modules.swap_new.viewmodels.SwapViewModel
import io.horizontalsystems.uniswapkit.UniswapKit
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

object SwapModule {

    @Parcelize
    data class CoinBalanceItem(
            val coin: Coin,
            val balance: BigDecimal?,
            val blockchainType: String?
    ) : Parcelable

    class Factory(private val fromCoin: Coin?) : ViewModelProvider.Factory {
        private val ethereumKit by lazy { App.ethereumKitManager.ethereumKit!! }
        private val uniswapKit by lazy { UniswapKit.getInstance(ethereumKit) }
        private val transactionService by lazy {
            val feeRateProvider = FeeRateProviderFactory.provider(App.appConfigProvider.ethereumCoin) as EthereumFeeRateProvider
            EthereumTransactionService(ethereumKit, feeRateProvider)
        }
        private val ethCoinService by lazy { CoinService(App.appConfigProvider.ethereumCoin, App.currencyManager, App.xRateManager) }
        private val uniswapRepository by lazy { UniswapRepository(uniswapKit) }
        private val allowanceService by lazy { SwapAllowanceService(uniswapRepository.routerAddress, App.adapterManager, ethereumKit) }
        private val service by lazy {
            SwapService(ethereumKit, tradeService, allowanceService, transactionService, App.adapterManager)
        }
        private val tradeService by lazy {
            SwapTradeService(uniswapRepository, fromCoin)
        }
        private val stringProvider by lazy {
            StringProvider(App.instance)
        }
        private val formatter by lazy {
            SwapItemFormatter(stringProvider, App.numberFormatter)
        }
        private val coinProvider by lazy {
            SwapCoinProvider(App.coinManager, App.walletManager, App.adapterManager)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            return when (modelClass) {
                SwapViewModel::class.java -> {
                    val uniswapKit = UniswapKit.getInstance(ethereumKit)

//                    val allowanceProvider = AllowanceProvider(App.adapterManager)
//                    val feeRateProvider = EthereumFeeRateProvider(App.feeRateProvider)
//                    val stringProvider = StringProvider(App.instance)
//
//                    val swapRepository = UniswapRepository(uniswapKit)
//                    val swapService = UniswapService(coinSending, swapRepository, allowanceProvider, App.walletManager, App.adapterManager, transactionService, ethereumKit, App.appConfigProvider.ethereumCoin)
//                    val formatter = SwapItemFormatter(stringProvider, App.numberFormatter)
//                    val confirmationPresenter = ConfirmationPresenter(swapService, stringProvider, formatter, ethCoinService)

                    SwapViewModel(service, tradeService, formatter) as T
                }
                SwapFromCoinCardViewModel::class.java -> {
                    SwapFromCoinCardViewModel(service, tradeService, coinProvider, formatter, stringProvider) as T
                }
                SwapToCoinCardViewModel::class.java -> {
                    SwapToCoinCardViewModel(service, tradeService, coinProvider, formatter, stringProvider) as T
                }
                SwapAllowanceViewModel::class.java -> {
                    SwapAllowanceViewModel(service, allowanceService, formatter, stringProvider) as T
                }
                EthereumFeeViewModel::class.java -> {
                    EthereumFeeViewModel(transactionService, ethCoinService) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

}
