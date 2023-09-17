package cash.p.terminal.modules.swapx.oneinch

import cash.p.terminal.core.subscribeIO
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.swap.oneinch.OneInchKitHelper
import cash.p.terminal.modules.swap.oneinch.OneInchSwapParameters
import cash.p.terminal.modules.swap.settings.oneinch.OneInchSwapSettingsModule.OneInchSwapSettings
import cash.p.terminal.modules.swapx.SwapXMainModule
import cash.p.terminal.modules.swapx.SwapXMainModule.SwapData
import cash.p.terminal.modules.swapx.SwapXMainModule.SwapResultState
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.oneinchkit.Quote
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

class OneInchTradeXService(
    private val oneInchKitHelper: OneInchKitHelper
) : SwapXMainModule.ISwapTradeXService {

    private var quoteDisposable: Disposable? = null

    override var state: SwapResultState = SwapResultState.NotReady()
        private set(value) {
            field = value
            _stateFlow.update { value }
        }

    private val _stateFlow = MutableStateFlow(state)
    override val stateFlow: StateFlow<SwapResultState>
        get() = _stateFlow

    var swapSettings: OneInchSwapSettings = OneInchSwapSettings()
        set(value) {
            field = value
//            syncQuote()
        }

    override fun stop() {
        clearDisposables()
    }

    fun onCleared() {
        clearDisposables()
    }

    private fun clearDisposables() {
        quoteDisposable?.dispose()
        quoteDisposable = null
    }

    override fun fetchSwapData(
        tokenFrom: Token?,
        tokenTo: Token?,
        amountFrom: BigDecimal?,
        amountTo: BigDecimal?,
        amountType: SwapMainModule.AmountType
    ) {
        quoteDisposable?.dispose()
        quoteDisposable = null

        val fromToken = tokenFrom ?: return
        val toToken = tokenTo ?: return

        if (amountFrom == null || amountFrom.compareTo(BigDecimal.ZERO) == 0) {
            state = SwapResultState.NotReady()
            return
        }

        state = SwapResultState.Loading

        quoteDisposable = oneInchKitHelper.getQuoteAsync(fromToken, toToken, amountFrom)
            .subscribeIO({ quote ->
                handle(quote, tokenFrom, tokenTo, amountFrom)
            }, { error ->
                state = SwapResultState.NotReady(listOf(error))
            })
    }

    private fun handle(quote: Quote, tokenFrom: Token, tokenTo: Token, amountFrom: BigDecimal) {
        val amountToBigDecimal = quote.toTokenAmount.abs().toBigDecimal().movePointLeft(quote.toToken.decimals).stripTrailingZeros()

        val parameters = OneInchSwapParameters(
            tokenFrom,
            tokenTo,
            amountFrom,
            amountToBigDecimal,
            swapSettings.slippage,
            swapSettings.recipient
        )

        state = SwapResultState.Ready(SwapData.OneInchData(parameters))
    }

}
