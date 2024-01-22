package cash.p.terminal.modules.swap.oneinch

import cash.p.terminal.core.App
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.swap.SwapMainModule.OneInchSwapParameters
import cash.p.terminal.modules.swap.SwapMainModule.SwapData
import cash.p.terminal.modules.swap.SwapMainModule.SwapResultState
import cash.p.terminal.modules.swap.SwapQuote
import cash.p.terminal.modules.swap.settings.oneinch.OneInchSwapSettingsModule
import cash.p.terminal.modules.swap.settings.oneinch.OneInchSwapSettingsModule.OneInchSwapSettings
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.oneinchkit.Quote
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

class OneInchTradeService : SwapMainModule.ISwapTradeService {
    private val oneInchKitHelper by lazy { OneInchKitHelper(App.appConfigProvider.oneInchApiKey) }

    private var quoteDisposable: Disposable? = null

    override var state: SwapResultState = SwapResultState.NotReady()
        private set(value) {
            field = value
            _stateFlow.update { value }
        }

    override val recipient: Address?
        get() = swapSettings.recipient

    override val slippage: BigDecimal
        get() = swapSettings.slippage

    private val _stateFlow = MutableStateFlow(state)
    override val stateFlow: StateFlow<SwapResultState>
        get() = _stateFlow

    private var swapSettings = OneInchSwapSettings()

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

    override suspend fun fetchQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal
    ): SwapQuote {
        val chain = App.evmBlockchainManager.getChain(tokenIn.blockchainType)

        val quote = oneInchKitHelper.getQuoteAsync(chain, tokenIn, tokenOut, amountIn).await()
        val amountOut = quote.toTokenAmount.abs().toBigDecimal().movePointLeft(quote.toToken.decimals).stripTrailingZeros()
        return SwapQuote(amountOut, listOf(), null)
    }

    override fun updateSwapSettings(recipient: Address?, slippage: BigDecimal?, ttl: Long?) {
        swapSettings = OneInchSwapSettings(
            recipient = recipient,
            slippage = slippage ?: OneInchSwapSettingsModule.defaultSlippage
        )
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
