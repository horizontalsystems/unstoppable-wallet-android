package io.horizontalsystems.bankwallet.modules.swap

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService
import io.horizontalsystems.bankwallet.core.fiat.FiatService
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.swap.coincard.ISwapCoinCardService
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapCoinCardViewModel
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapFromCoinCardService
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapToCoinCardService
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchFragment
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapFragment
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.Observable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.util.*

object SwapMainModule {

    const val coinCardTypeFrom = "coinCardTypeFrom"
    const val coinCardTypeTo = "coinCardTypeTo"

    private const val tokenFromKey = "tokenFromKey"

    fun prepareParams(tokenFrom: Token) = bundleOf(tokenFromKey to tokenFrom)

    interface ISwapProvider : Parcelable {
        val id: String
        val title: String
        val url: String
        val fragment: SwapBaseFragment

        fun supports(blockchainType: BlockchainType): Boolean
    }

    @Parcelize
    object UniswapProvider : ISwapProvider {
        override val id = "uniswap"
        override val title = "Uniswap"
        override val url = "https://uniswap.org/"
        override val fragment: SwapBaseFragment
            get() = UniswapFragment()

        override fun supports(blockchainType: BlockchainType): Boolean {
            return blockchainType == BlockchainType.Ethereum
        }
    }

    @Parcelize
    object PancakeSwapProvider : ISwapProvider {
        override val id = "pancake"
        override val title = "PancakeSwap"
        override val url = "https://pancakeswap.finance/"
        override val fragment: SwapBaseFragment
            get() = UniswapFragment()

        override fun supports(blockchainType: BlockchainType): Boolean {
            return blockchainType == BlockchainType.BinanceSmartChain
        }
    }

    @Parcelize
    object OneInchProvider : ISwapProvider {
        override val id = "oneinch"
        override val title = "1inch"
        override val url = "https://app.1inch.io/"
        override val fragment: SwapBaseFragment
            get() = OneInchFragment()

        override fun supports(blockchainType: BlockchainType) = when (blockchainType) {
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.ArbitrumOne -> true
            else -> false
        }
    }

    @Parcelize
    object QuickSwapProvider : ISwapProvider {
        override val id = "quickswap"
        override val title = "QuickSwap"
        override val url = "https://quickswap.exchange/"
        override val fragment: SwapBaseFragment
            get() = UniswapFragment()

        override fun supports(blockchainType: BlockchainType): Boolean {
            return blockchainType == BlockchainType.Polygon
        }
    }

    @Parcelize
    class Dex(val blockchain: Blockchain, val provider: ISwapProvider) : Parcelable {
        val blockchainType get() = blockchain.type
    }

    @Parcelize
    enum class AmountType : Parcelable {
        ExactFrom, ExactTo
    }

    interface ISwapTradeService {
        val tokenFrom: Token?
        val tokenFromObservable: Observable<Optional<Token>>
        val amountFrom: BigDecimal?
        val amountFromObservable: Observable<Optional<BigDecimal>>

        val tokenTo: Token?
        val tokenToObservable: Observable<Optional<Token>>
        val amountTo: BigDecimal?
        val amountToObservable: Observable<Optional<BigDecimal>>

        val amountType: AmountType
        val amountTypeObservable: Observable<AmountType>

        fun enterTokenFrom(token: Token?)
        fun enterAmountFrom(amount: BigDecimal?)

        fun enterTokenTo(token: Token?)
        fun enterAmountTo(amount: BigDecimal?)

        fun restoreState(swapProviderState: SwapProviderState)

        fun switchCoins()
    }

    interface ISwapService {
        val balanceFrom: BigDecimal?
        val balanceFromObservable: Observable<Optional<BigDecimal>>

        val balanceTo: BigDecimal?
        val balanceToObservable: Observable<Optional<BigDecimal>>

        val errors: List<Throwable>
        val errorsObservable: Observable<List<Throwable>>

        fun start()
        fun stop()
    }

    sealed class SwapError : Throwable() {
        object InsufficientBalanceFrom : SwapError()
        object InsufficientAllowance : SwapError()
        object RevokeAllowanceRequired : SwapError()
    }

    @Parcelize
    data class CoinBalanceItem(
        val token: Token,
        val balance: BigDecimal?,
        val fiatBalanceValue: CurrencyValue?,
    ) : Parcelable

    enum class ApproveStep {
        NA, ApproveRequired, Approving, Approved
    }

    @Parcelize
    data class SwapProviderState(
        val tokenFrom: Token? = null,
        val tokenTo: Token? = null,
        val amountFrom: BigDecimal? = null,
        val amountTo: BigDecimal? = null,
        val amountType: AmountType = AmountType.ExactFrom
    ) : Parcelable

    class Factory(arguments: Bundle) : ViewModelProvider.Factory {
        private val tokenFrom: Token? = arguments.getParcelable(tokenFromKey)
        private val swapProviders: List<ISwapProvider> =
            listOf(UniswapProvider, PancakeSwapProvider, OneInchProvider, QuickSwapProvider)

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            return when (modelClass) {
                SwapMainViewModel::class.java -> {
                    SwapMainViewModel(
                        SwapMainService(
                            tokenFrom,
                            swapProviders,
                            App.localStorage
                        )
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }

    }

    class CoinCardViewModelFactory(
        owner: SavedStateRegistryOwner,
        private val dex: Dex,
        private val service: ISwapService,
        private val tradeService: ISwapTradeService
    ) : AbstractSavedStateViewModelFactory(owner, null) {
        private val switchService by lazy {
            AmountTypeSwitchService()
        }
        private val fromCoinCardService by lazy {
            SwapFromCoinCardService(service, tradeService)
        }
        private val toCoinCardService by lazy {
            SwapToCoinCardService(service, tradeService)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            return when (modelClass) {
                SwapCoinCardViewModel::class.java -> {
                    val fiatService = FiatService(switchService, App.currencyManager, App.marketKit)
                    val coinCardService: ISwapCoinCardService
                    var maxButtonEnabled = false
                    val resetAmountOnCoinSelect: Boolean

                    if (key == coinCardTypeFrom) {
                        coinCardService = fromCoinCardService
                        switchService.fromListener = fiatService
                        maxButtonEnabled = true
                        resetAmountOnCoinSelect = true
                    } else {
                        coinCardService = toCoinCardService
                        switchService.toListener = fiatService
                        resetAmountOnCoinSelect = false
                    }
                    val formatter = SwapViewItemHelper(App.numberFormatter)
                    SwapCoinCardViewModel(
                        coinCardService,
                        fiatService,
                        switchService,
                        maxButtonEnabled,
                        formatter,
                        resetAmountOnCoinSelect,
                        dex
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }

    }


}

sealed class SwapActionState {
    object Hidden : SwapActionState()
    class Enabled(val buttonTitle: String) : SwapActionState()
    class Disabled(val buttonTitle: String) : SwapActionState();

    val title: String
        get() = when (this) {
            is Enabled -> this.buttonTitle
            is Disabled -> this.buttonTitle
            else -> ""
        }
}

data class SwapButtons(
    val revoke: SwapActionState,
    val approve: SwapActionState,
    val proceed: SwapActionState
)