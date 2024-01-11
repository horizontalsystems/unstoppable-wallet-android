package io.horizontalsystems.bankwallet.modules.swapxxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SwapViewModel(private val swapProvidersManager: SwapProvidersManager) : ViewModel() {
    private var spendingCoinAmount: BigDecimal? = null
    private var tokenFrom: Token? = null
    private var tokenTo: Token? = null
    private var calculating = false
    private var quotes: List<SwapProviderQuote> = listOf()
    private var bestQuote: SwapProviderQuote? = null
    private var selectedQuote: SwapProviderQuote? = null

    var uiState: SwapUiState by mutableStateOf(
        SwapUiState(
            spendingCoinAmount = spendingCoinAmount,
            tokenFrom = tokenFrom,
            tokenTo = tokenTo,
            calculating = calculating,
            swapEnabled = isSwapEnabled(),
            quotes = quotes,
            bestQuote = bestQuote,
            selectedQuote = selectedQuote,
        )
    )
        private set

    private var calculatingJob: Job? = null

    fun onEnterAmount(v: BigDecimal?) {
        spendingCoinAmount = v

        runQuotation()
    }

    fun onSelectTokenFrom(token: Token) {
        tokenFrom = token

        runQuotation()
    }

    fun onSelectTokenTo(token: Token) {
        tokenTo = token

        runQuotation()
    }

    fun onSwitchPairs() {
        val tmpTokenFrom = tokenFrom

        tokenFrom = tokenTo
        tokenTo = tmpTokenFrom

        spendingCoinAmount = selectedQuote?.quote?.amountOut

        runQuotation()
    }

    fun onSelectQuote(quote: SwapProviderQuote) {
        selectedQuote = quote

        emitState()
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = SwapUiState(
                spendingCoinAmount = spendingCoinAmount,
                tokenFrom = tokenFrom,
                tokenTo = tokenTo,
                calculating = calculating,
                swapEnabled = isSwapEnabled(),
                quotes = quotes,
                bestQuote = bestQuote,
                selectedQuote = selectedQuote,
            )
        }
    }

    private fun isSwapEnabled() = selectedQuote != null

    private fun runQuotation() {
        quotes = listOf()
        bestQuote = null
        selectedQuote = null
        calculating = false
        emitState()

        calculatingJob?.cancel()

        val spendingCoinAmount = spendingCoinAmount
        val tokenFrom = tokenFrom
        val tokenTo = tokenTo

        if (spendingCoinAmount != null && spendingCoinAmount > BigDecimal.ZERO && tokenFrom != null && tokenTo != null) {
            calculatingJob = viewModelScope.launch(Dispatchers.Default) {
                calculating = true
                emitState()

                quotes = swapProvidersManager.getQuotes(tokenFrom, tokenTo, spendingCoinAmount).sortedByDescending { it.quote.amountOut }
                bestQuote = quotes.firstOrNull()
                selectedQuote = bestQuote
                calculating = false
                emitState()
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
    val spendingCoinAmount: BigDecimal?,
    val tokenFrom: Token?,
    val tokenTo: Token?,
    val calculating: Boolean,
    val swapEnabled: Boolean,
    val quotes: List<SwapProviderQuote>,
    val bestQuote: SwapProviderQuote?,
    val selectedQuote: SwapProviderQuote?,
) {
    val prices: Pair<String, String>?
        get() {
            val amountIn = spendingCoinAmount ?: return null
            val amountOut = selectedQuote?.quote?.amountOut ?: return null
            val tokenFrom = tokenFrom ?: return null
            val tokenTo = tokenTo ?: return null

            val numberFormatter = App.numberFormatter

            val price = amountOut.div(amountIn).stripTrailingZeros()
            val from = numberFormatter.formatCoinShort(BigDecimal.ONE, tokenFrom.coin.code, 0)
            val to = numberFormatter.formatCoinShort(price, tokenTo.coin.code, tokenTo.decimals)
            val priceStr = "$from = $to"

            val priceInverted = amountIn.div(amountOut).stripTrailingZeros()
            val fromInverted = numberFormatter.formatCoinShort(BigDecimal.ONE, tokenTo.coin.code, 0)
            val toInverted = numberFormatter.formatCoinShort(priceInverted, tokenFrom.coin.code, tokenFrom.decimals)
            val priceInvertedStr = "$fromInverted = $toInverted"

            return Pair(priceStr, priceInvertedStr)
        }
}
