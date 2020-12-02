package io.horizontalsystems.bankwallet.modules.swap_new

import android.os.Parcelable
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.ethereum.EthereumTransactionService
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.core.providers.EthereumFeeRateProvider
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.provider.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.view.SwapItemFormatter
import io.horizontalsystems.bankwallet.modules.swap_new.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap_new.coincard.SwapCoinCardViewModel
import io.horizontalsystems.bankwallet.modules.swap_new.coincard.SwapFromCoinCardService
import io.horizontalsystems.bankwallet.modules.swap_new.coincard.SwapToCoinCardService
import io.horizontalsystems.bankwallet.modules.swap_new.coincard.SwapCoinProvider
import io.horizontalsystems.bankwallet.modules.swap_new.repositories.UniswapRepository
import io.horizontalsystems.bankwallet.modules.swap_new.allowance.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap_new.allowance.SwapPendingAllowanceService
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

    data class ConfirmationAmountViewItem(
            val payTitle: String,
            val payValue: String?,
            val getTitle: String,
            val getValue: String?
    )

    data class ConfirmationAdditionalViewItem(val title: String, val value: String?)

    data class GuaranteedAmountViewItem(val title: String, val value: String)

    data class PriceImpactViewItem(val level: SwapTradeService.PriceImpactLevel, val value: String)

    class Factory(
            owner: SavedStateRegistryOwner,
            private val fromCoin: Coin?
    ) : AbstractSavedStateViewModelFactory(owner, null) {
        private val ethereumKit by lazy { App.ethereumKitManager.ethereumKit!! }
        private val uniswapKit by lazy { UniswapKit.getInstance(ethereumKit) }
        private val transactionService by lazy {
            val feeRateProvider = FeeRateProviderFactory.provider(App.appConfigProvider.ethereumCoin) as EthereumFeeRateProvider
            EthereumTransactionService(ethereumKit, feeRateProvider)
        }
        private val ethCoinService by lazy { CoinService(App.appConfigProvider.ethereumCoin, App.currencyManager, App.xRateManager) }
        private val uniswapRepository by lazy { UniswapRepository(uniswapKit) }
        private val allowanceService by lazy { SwapAllowanceService(uniswapRepository.routerAddress, App.adapterManager, ethereumKit) }
        private val pendingAllowanceService by lazy { SwapPendingAllowanceService(App.adapterManager, allowanceService) }
        private val service by lazy {
            SwapService(ethereumKit, tradeService, allowanceService, pendingAllowanceService, transactionService, App.adapterManager)
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
        private val fromCoinCardService by lazy {
            SwapFromCoinCardService(service, tradeService, coinProvider)
        }
        private val toCoinCardService by lazy {
            SwapToCoinCardService(service, tradeService, coinProvider)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {

            return when (modelClass) {
                SwapViewModel::class.java -> {
                    SwapViewModel(service, tradeService, pendingAllowanceService, ethCoinService, formatter, stringProvider) as T
                }
                SwapCoinCardViewModel::class.java -> {
                    val coinCardService = if (key == coinCardTypeFrom) fromCoinCardService else toCoinCardService
                    SwapCoinCardViewModel(coinCardService, formatter, stringProvider) as T
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

        companion object {
            const val coinCardTypeFrom = "coinCardTypeFrom"
            const val coinCardTypeTo = "coinCardTypeTo"
        }

    }

}
