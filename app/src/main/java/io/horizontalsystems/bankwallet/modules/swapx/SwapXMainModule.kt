package io.horizontalsystems.bankwallet.modules.swapx

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService
import io.horizontalsystems.bankwallet.core.fiat.FiatService
import io.horizontalsystems.bankwallet.modules.swap.SwapButtons
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper
import io.horizontalsystems.bankwallet.modules.swap.coincard.InputParams
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchSwapParameters
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapModule
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapTradeService.PriceImpactLevel
import io.horizontalsystems.bankwallet.modules.swapx.allowance.SwapAllowanceServiceX
import io.horizontalsystems.bankwallet.modules.swapx.allowance.SwapAllowanceViewModelX
import io.horizontalsystems.bankwallet.modules.swapx.allowance.SwapPendingAllowanceServiceX
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.uniswapkit.models.TradeData
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.util.*

object SwapXMainModule {

    private const val tokenFromKey = "tokenFromKey"

    fun prepareParams(tokenFrom: Token) = bundleOf(tokenFromKey to tokenFrom)

    data class ProviderViewItem(
        val provider: ISwapProvider,
        val selected: Boolean,
    )

    class Factory(arguments: Bundle) : ViewModelProvider.Factory {
        private val tokenFrom: Token? = arguments.getParcelable(tokenFromKey)
        private val swapProviders: List<ISwapProvider> = listOf(
            UniswapProvider,
            PancakeSwapProvider,
            OneInchProvider,
            QuickSwapProvider
        )
        private val switchService by lazy { AmountTypeSwitchService() }
        private val swapMainXService by lazy { SwapXMainService(tokenFrom, swapProviders, App.localStorage) }
        private val evmKit: EthereumKit by lazy { App.evmBlockchainManager.getEvmKitManager(swapMainXService.dex.blockchainType).evmKitWrapper?.evmKit!! }
        private val allowanceService by lazy { SwapAllowanceServiceX(App.adapterManager, evmKit) }
        private val pendingAllowanceService by lazy { SwapPendingAllowanceServiceX(App.adapterManager, allowanceService) }
        private val errorShareService by lazy { ErrorShareService() }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            return when (modelClass) {
                SwapXMainViewModel::class.java -> {
                    val fromFiatService = FiatService(switchService, App.currencyManager, App.marketKit)
                    switchService.fromListener = fromFiatService
                    val toFiatService = FiatService(switchService, App.currencyManager, App.marketKit)
                    switchService.toListener = toFiatService

                    val fromTokenService = SwapTokenService(
                        switchService = switchService,
                        fiatService = fromFiatService,
                        resetAmountOnCoinSelect = true,
                        initialToken = tokenFrom
                    )
                    val toTokenService = SwapTokenService(
                        switchService = switchService,
                        fiatService = toFiatService,
                        resetAmountOnCoinSelect = false,
                        initialToken = null
                    )

                    val formatter = SwapViewItemHelper(App.numberFormatter)
                    SwapXMainViewModel(
                        formatter,
                        swapMainXService,
                        switchService,
                        fromTokenService,
                        toTokenService,
                        allowanceService,
                        pendingAllowanceService,
                        errorShareService,
                        TimerService(evmKit),
                        App.currencyManager,
                        App.adapterManager
                    ) as T
                }

                SwapAllowanceViewModelX::class.java -> {
                    SwapAllowanceViewModelX(
                        errorShareService,
                        allowanceService,
                        pendingAllowanceService,
                        SwapViewItemHelper(App.numberFormatter)
                    ) as T
                }

                else -> throw IllegalArgumentException()
            }
        }

    }

    interface ISwapTradeXService {
        val state: SwapResultState
        val stateFlow: Flow<SwapResultState>

        fun stop()
        fun fetchSwapData(
            tokenFrom: Token?,
            tokenTo: Token?,
            amountFrom: BigDecimal?,
            amountTo: BigDecimal?,
            amountType: SwapMainModule.AmountType
        )
    }

    data class SwapState(
        val dex: Dex,
        val providerViewItems: List<ProviderViewItem>,
        val availableBalance: String?,
        val amountTypeSelect: Select<AmountTypeItem>,
        val amountTypeSelectEnabled: Boolean,
        val fromState: SwapXCoinCardViewState,
        val toState: SwapXCoinCardViewState,
        val tradeView: TradeViewX?,
        val tradePriceExpiration: Float?,
        val error: String?,
        val buttons: SwapButtons,
        val hasNonZeroBalance: Boolean?,
    )

    data class SwapXCoinCardViewState(
        val token: Token?,
        val uuid: Long,
        val inputState: SwapXAmountInputState,
    )

    data class SwapXAmountInputState(
        val amount: String,
        val secondaryInfo: String,
        val inputParams: InputParams,
        val validDecimals: Int,
        val amountEnabled: Boolean,
        val dimAmount: Boolean,
    )

    sealed class AmountTypeItem : WithTranslatableTitle {
        object Coin : AmountTypeItem()
        class Currency(val name: String) : AmountTypeItem()

        override val title: TranslatableString
            get() = when (this) {
                Coin -> TranslatableString.ResString(R.string.Swap_AmountTypeCoin)
                is Currency -> TranslatableString.PlainString(name)
            }

        override fun equals(other: Any?): Boolean {
            return other is Coin && this is Coin || other is Currency && this is Currency && other.name == this.name
        }

        override fun hashCode() = when (this) {
            Coin -> javaClass.hashCode()
            is Currency -> name.hashCode()
        }
    }

    sealed class SwapResultState {
        object Loading : SwapResultState()
        class Ready(val swapData: SwapData) : SwapResultState()
        class NotReady(val errors: List<Throwable> = listOf()) : SwapResultState()
    }

    sealed class SwapData {
        data class OneInchData(val data: OneInchSwapParameters) : SwapData()
        data class UniswapData(val data: TradeData) : SwapData() {
            private val warningPriceImpact = BigDecimal(1)
            private val forbiddenPriceImpact = BigDecimal(5)

            val priceImpactLevel: PriceImpactLevel? = data.priceImpact?.let {
                when {
                    it >= BigDecimal.ZERO && it < warningPriceImpact -> PriceImpactLevel.Normal
                    it >= warningPriceImpact && it < forbiddenPriceImpact -> PriceImpactLevel.Warning
                    else -> PriceImpactLevel.Forbidden
                }
            }
        }
    }

    data class TradeViewX(
        val providerTradeData: ProviderTradeData,
        val expired: Boolean = false
    )

    sealed class ProviderTradeData {
        class OneInchTradeViewItem(
            val primaryPrice: String? = null,
            val secondaryPrice: String? = null,
        ) : ProviderTradeData()

        class UniswapTradeViewItem(
            val primaryPrice: String? = null,
            val secondaryPrice: String? = null,
            val priceImpact: UniswapModule.PriceImpactViewItem? = null,
            val guaranteedAmount: UniswapModule.GuaranteedAmountViewItem? = null,
        ) : ProviderTradeData()
    }

    @Parcelize
    class Dex(val blockchain: Blockchain, val provider: ISwapProvider) : Parcelable {
        val blockchainType get() = blockchain.type
    }

    interface ISwapProvider : Parcelable {
        val id: String
        val title: String
        val url: String

        fun supports(blockchainType: BlockchainType): Boolean
    }

    @Parcelize
    object UniswapProvider : ISwapProvider {
        override val id get() = "uniswap"
        override val title get() = "Uniswap"
        override val url get() = "https://uniswap.org/"

        override fun supports(blockchainType: BlockchainType): Boolean {
            return blockchainType == BlockchainType.Ethereum
        }
    }

    @Parcelize
    object PancakeSwapProvider : ISwapProvider {
        override val id get() = "pancake"
        override val title get() = "PancakeSwap"
        override val url get() = "https://pancakeswap.finance/"

        override fun supports(blockchainType: BlockchainType): Boolean {
            return blockchainType == BlockchainType.BinanceSmartChain
        }
    }

    @Parcelize
    object OneInchProvider : ISwapProvider {
        override val id get() = "oneinch"
        override val title get() = "1inch"
        override val url get() = "https://app.1inch.io/"

        override fun supports(blockchainType: BlockchainType) = when (blockchainType) {
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
            BlockchainType.ArbitrumOne -> true

            else -> false
        }
    }

    @Parcelize
    object QuickSwapProvider : ISwapProvider {
        override val id get() = "quickswap"
        override val title get() = "QuickSwap"
        override val url get() = "https://quickswap.exchange/"

        override fun supports(blockchainType: BlockchainType): Boolean {
            return blockchainType == BlockchainType.Polygon
        }
    }

    @Parcelize
    data class ApproveData(
        val dex: Dex,
        val token: Token,
        val spenderAddress: String,
        val amount: BigDecimal,
        val allowance: BigDecimal
    ) : Parcelable

}
