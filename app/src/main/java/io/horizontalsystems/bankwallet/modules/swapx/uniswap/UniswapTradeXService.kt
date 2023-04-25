package io.horizontalsystems.bankwallet.modules.swapx.uniswap

import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.AmountType
import io.horizontalsystems.bankwallet.modules.swap.providers.UniswapProvider
import io.horizontalsystems.bankwallet.modules.swapx.SwapXMainModule
import io.horizontalsystems.bankwallet.modules.swapx.SwapXMainModule.SwapData.UniswapData
import io.horizontalsystems.bankwallet.modules.swapx.SwapXMainModule.SwapResultState
import io.horizontalsystems.bankwallet.modules.swapx.settings.uniswap.SwapTradeOptions
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.TradeError
import io.horizontalsystems.uniswapkit.models.SwapData
import io.horizontalsystems.uniswapkit.models.TradeData
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.models.TradeType
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal


class UniswapTradeXService(
    private val uniswapProvider: UniswapProvider
) : SwapXMainModule.ISwapTradeXService {

    private var swapDataDisposable: Disposable? = null
    private var swapData: SwapData? = null

    override var state: SwapResultState = SwapResultState.NotReady()
        private set(value) {
            field = value
            _stateFlow.update { value }
        }

    override val recipient: Address?
        get() = tradeOptions.recipient

    private val _stateFlow = MutableStateFlow(state)
    override val stateFlow: StateFlow<SwapResultState>
        get() = _stateFlow

    var tradeOptions: SwapTradeOptions = SwapTradeOptions()
        set(value) {
            field = value
        }

    override fun stop() {
        clearDisposables()
    }

    override fun fetchSwapData(
        tokenFrom: Token?,
        tokenTo: Token?,
        amountFrom: BigDecimal?,
        amountTo: BigDecimal?,
        amountType: AmountType
    ) {
        if (tokenFrom == null || tokenTo == null) {
            state = SwapResultState.NotReady()
            return
        }

        state = SwapResultState.Loading

        swapDataDisposable?.dispose()
        swapDataDisposable = null

        swapDataDisposable = uniswapProvider.swapDataSingle(tokenFrom, tokenTo)
            .subscribeOn(Schedulers.io())
            .subscribe({
                swapData = it
                syncTradeData(amountType, amountFrom, amountTo, tokenFrom, tokenTo)
            }, { error ->
                state = SwapResultState.NotReady(listOf(error))
            })
    }

    override fun updateSwapSettings(recipient: Address?, slippage: BigDecimal?, ttl: Long?) {
        tradeOptions = SwapTradeOptions(
            slippage ?: TradeOptions.defaultAllowedSlippage,
            ttl ?: TradeOptions.defaultTtl,
            recipient
        )
    }

    @Throws
    fun transactionData(tradeData: TradeData): TransactionData {
        return uniswapProvider.transactionData(tradeData)
    }

    private fun clearDisposables() {
        swapDataDisposable?.dispose()
        swapDataDisposable = null
    }

    private fun syncTradeData(amountType: AmountType, amountFrom: BigDecimal?, amountTo: BigDecimal?, tokenFrom: Token, tokenTo: Token) {
        val swapData = swapData ?: return

        val amount = if (amountType == AmountType.ExactFrom) amountFrom else amountTo

        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            state = SwapResultState.NotReady()
            return
        }

        try {
            val tradeType = when (amountType) {
                AmountType.ExactFrom -> TradeType.ExactIn
                AmountType.ExactTo -> TradeType.ExactOut
            }
            val tradeData = uniswapProvider.tradeData(swapData, amount, tradeType, tradeOptions.tradeOptions)
            state = SwapResultState.Ready(UniswapData(tradeData))
        } catch (e: Throwable) {
            val error = when {
                e is TradeError.TradeNotFound && isEthWrapping(tokenFrom, tokenTo) -> TradeServiceError.WrapUnwrapNotAllowed
                else -> e
            }
            state = SwapResultState.NotReady(listOf(error))
        }
    }

    private val wethAddressHex = uniswapProvider.wethAddress.hex
    private val Token.isWeth: Boolean
        get() = (type as? TokenType.Eip20)?.address?.equals(wethAddressHex, ignoreCase = true) ?: false
    private val Token.isNative: Boolean
        get() = type == TokenType.Native

    private fun isEthWrapping(tokenFrom: Token?, tokenTo: Token?) =
        when {
            tokenFrom == null || tokenTo == null -> false
            else -> {
                tokenFrom.isNative && tokenTo.isWeth || tokenTo.isNative && tokenFrom.isWeth
            }
        }

    sealed class TradeServiceError : Throwable() {
        object WrapUnwrapNotAllowed : TradeServiceError()
    }

}
