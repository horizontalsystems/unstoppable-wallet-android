package io.horizontalsystems.bankwallet.modules.swap

import android.os.Parcelable
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService
import io.horizontalsystems.bankwallet.core.fiat.FiatService
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.coincard.*
import io.horizontalsystems.bankwallet.modules.swap.providers.UniswapProvider
import io.horizontalsystems.ethereumkit.core.EthereumKit
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

    enum class Dex {
        Uniswap, PancakeSwap;

        val evmKit: EthereumKit?
            get() = when (this) {
                Uniswap -> App.ethereumKitManager.evmKit
                PancakeSwap -> App.binanceSmartChainKitManager.evmKit
            }

        val coin: Coin
            get() = when (this) {
                Uniswap -> App.appConfigProvider.ethereumCoin
                PancakeSwap -> App.appConfigProvider.binanceSmartChainCoin
            }
    }

    class Factory(
            owner: SavedStateRegistryOwner,
            private val fromCoin: Coin
    ) : AbstractSavedStateViewModelFactory(owner, null) {

        private val dex: Dex by lazy {
            when (fromCoin.type) {
                CoinType.Ethereum, is CoinType.Erc20 -> Dex.Uniswap
                CoinType.BinanceSmartChain, is CoinType.Bep20 -> Dex.PancakeSwap
                else -> throw IllegalArgumentException()
            }
        }

        private val evmKit: EthereumKit by lazy { dex.evmKit!! }
        private val uniswapKit by lazy { UniswapKit.getInstance(evmKit) }
        private val feeRateProvider by lazy { FeeRateProviderFactory.provider(dex.coin)!! }
        private val transactionService by lazy { EvmTransactionService(evmKit, feeRateProvider, 20) }
        private val coinService by lazy { CoinService(dex.coin, App.currencyManager, App.xRateManager) }
        private val uniswapProvider by lazy { UniswapProvider(uniswapKit) }
        private val allowanceService by lazy { SwapAllowanceService(uniswapProvider.routerAddress, App.adapterManager, evmKit) }
        private val pendingAllowanceService by lazy { SwapPendingAllowanceService(App.adapterManager, allowanceService) }
        private val service by lazy {
            SwapService(dex, evmKit, tradeService, allowanceService, pendingAllowanceService, transactionService, App.adapterManager)
        }
        private val tradeService by lazy {
            SwapTradeService(evmKit, uniswapProvider, fromCoin)
        }
        private val stringProvider by lazy {
            StringProvider(App.instance)
        }
        private val formatter by lazy {
            SwapViewItemHelper(stringProvider, App.numberFormatter)
        }
        private val coinProvider by lazy {
            SwapCoinProvider(dex, App.coinManager, App.walletManager, App.adapterManager)
        }
        private val fromCoinCardService by lazy {
            SwapFromCoinCardService(service, tradeService, coinProvider)
        }
        private val toCoinCardService by lazy {
            SwapToCoinCardService(service, tradeService, coinProvider)
        }
        private val switchService by lazy {
            AmountTypeSwitchService()
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {

            return when (modelClass) {
                SwapViewModel::class.java -> {
                    SwapViewModel(service, tradeService, allowanceService, pendingAllowanceService, coinService, formatter, stringProvider) as T
                }
                SwapCoinCardViewModel::class.java -> {
                    val fiatService = FiatService(switchService, App.currencyManager, App.xRateManager)
                    val coinCardService: ISwapCoinCardService
                    var maxButtonEnabled = false

                    if (key == coinCardTypeFrom) {
                        coinCardService = fromCoinCardService
                        switchService.fromListener = fiatService
                        maxButtonEnabled = true
                    } else {
                        coinCardService = toCoinCardService
                        switchService.toListener = fiatService
                    }
                    SwapCoinCardViewModel(coinCardService, fiatService, switchService, maxButtonEnabled, formatter, stringProvider) as T
                }
                SwapAllowanceViewModel::class.java -> {
                    SwapAllowanceViewModel(service, allowanceService, pendingAllowanceService, formatter, stringProvider) as T
                }
                EthereumFeeViewModel::class.java -> {
                    EthereumFeeViewModel(transactionService, coinService) as T
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
