package io.horizontalsystems.bankwallet.modules.swapxxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class SwapViewModel(private val swapProvidersManager: SwapProvidersManager) : ViewModel() {
    private val quoteLifetime = 30000L
    private var amountIn: BigDecimal? = null
    private var tokenIn: Token? = null
    private var tokenOut: Token? = null
    private var calculating = false
    private var quotes: List<SwapProviderQuote> = listOf()
    private var bestQuote: SwapProviderQuote? = null
    private var selectedProvider: SwapMainModule.ISwapProvider? = null
    private var amountOut: BigDecimal? = null

    var uiState: SwapUiState by mutableStateOf(
        SwapUiState(
            amountIn = amountIn,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            calculating = calculating,
            swapEnabled = isSwapEnabled(),
            quotes = quotes,
            bestQuote = bestQuote,
            selectedProvider = selectedProvider,
            quoteLifetime = quoteLifetime,
            amountOut = amountOut,
        )
    )
        private set

    private var calculatingJob: Job? = null

    fun onEnterAmount(v: BigDecimal?) {
        amountIn = v

        runQuotation()
    }

    fun onSelectTokenIn(token: Token) {
        tokenIn = token

        runQuotation()
    }

    fun onSelectTokenOut(token: Token) {
        tokenOut = token

        runQuotation()
    }

    fun onSwitchPairs() {
        val tmpTokenIn = tokenIn

        tokenIn = tokenOut
        tokenOut = tmpTokenIn

        amountIn = amountOut

        runQuotation()
    }

    fun onSelectQuote(quote: SwapProviderQuote) {
        selectedProvider = quote.provider
        amountOut = quote.quote.amountOut

        emitState()
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = SwapUiState(
                amountIn = amountIn,
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                calculating = calculating,
                swapEnabled = isSwapEnabled(),
                quotes = quotes,
                bestQuote = bestQuote,
                selectedProvider = selectedProvider,
                quoteLifetime = quoteLifetime,
                amountOut = amountOut
            )
        }
    }

    private fun onQuoteExpired() {
        runQuotation()
    }

    private fun isSwapEnabled() = amountOut != null

    private fun runQuotation() {
        quotes = listOf()
        bestQuote = null
        selectedProvider = null
        amountOut = null
        calculating = false
        emitState()

        calculatingJob?.cancel()

        val amountIn = amountIn
        val tokenIn = tokenIn
        val tokenOut = tokenOut

        if (amountIn != null && amountIn > BigDecimal.ZERO && tokenIn != null && tokenOut != null) {
            calculatingJob = viewModelScope.launch(Dispatchers.Default) {
                calculating = true
                emitState()

                quotes = swapProvidersManager.getQuotes(tokenIn, tokenOut, amountIn).sortedByDescending { it.quote.amountOut }
                bestQuote = quotes.firstOrNull()
                selectedProvider = bestQuote?.provider
                amountOut = bestQuote?.quote?.amountOut
                calculating = false
                emitState()

                delay(quoteLifetime)
                onQuoteExpired()
            }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapViewModel(SwapProvidersManager()) as T
        }
    }
}

data class SwapUiState(
    val amountIn: BigDecimal?,
    val tokenIn: Token?,
    val tokenOut: Token?,
    val calculating: Boolean,
    val swapEnabled: Boolean,
    val quotes: List<SwapProviderQuote>,
    val bestQuote: SwapProviderQuote?,
    val selectedProvider: SwapMainModule.ISwapProvider?,
    val quoteLifetime: Long,
    val amountOut: BigDecimal?
) {
    val prices: Pair<String, String>?
        get() {
            val amountIn = amountIn ?: return null
            val amountOut = amountOut ?: return null
            val tokenIn = tokenIn ?: return null
            val tokenOut = tokenOut ?: return null

            val numberFormatter = App.numberFormatter

            val price = amountOut.divide(amountIn, tokenOut.decimals, RoundingMode.HALF_EVEN).stripTrailingZeros()
            val from = numberFormatter.formatCoinShort(BigDecimal.ONE, tokenIn.coin.code, 0)
            val to = numberFormatter.formatCoinShort(price, tokenOut.coin.code, tokenOut.decimals)
            val priceStr = "$from = $to"

            val priceInverted = amountIn.divide(amountOut, tokenIn.decimals, RoundingMode.HALF_EVEN).stripTrailingZeros()
            val fromInverted = numberFormatter.formatCoinShort(BigDecimal.ONE, tokenOut.coin.code, 0)
            val toInverted = numberFormatter.formatCoinShort(priceInverted, tokenIn.coin.code, tokenIn.decimals)
            val priceInvertedStr = "$fromInverted = $toInverted"

            return Pair(priceStr, priceInvertedStr)
        }
}
