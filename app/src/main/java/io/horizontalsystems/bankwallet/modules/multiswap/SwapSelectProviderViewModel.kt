package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import java.math.BigDecimal
import java.math.RoundingMode

class SwapSelectProviderViewModel(
    private val quotes: List<SwapProviderQuote>,
    private val quote: SwapProviderQuote?
) : ViewModelUiState<SwapSelectProviderUiState>() {
    private val normalPriceImpact = BigDecimal(1)
    private val warningPriceImpact = BigDecimal(6)
    private val highPriceImpact = BigDecimal(11)
    private val forbiddenPriceImpact = BigDecimal(50)

    private val currencyManager = App.currencyManager
    private val marketKit = App.marketKit

    private val currency = currencyManager.baseCurrency
    private var tokenIn = quotes.first().tokenIn
    private var amountIn = quotes.first().amountIn
    private var tokenOut = quotes.first().tokenOut
    private var rateTokenIn: BigDecimal? = marketKit.coinPrice(tokenIn.coin.uid, currency.code)?.value
    private var rateTokenOut: BigDecimal? = marketKit.coinPrice(tokenOut.coin.uid, currency.code)?.value
    private var quoteViewItems = getViewItems(quotes.sorted())

    init {
        viewModelScope.launch {
            marketKit.coinPriceObservable("swap-providers", tokenIn.coin.uid, currency.code)
                .asFlow()
                .collect {
                    rateTokenIn = it.value
                    quoteViewItems = getViewItems(quotes.sorted())
                    emitState()
                }
        }
        viewModelScope.launch {
            marketKit.coinPriceObservable("swap-providers", tokenOut.coin.uid, currency.code)
                .asFlow()
                .collect {
                    rateTokenOut = it.value
                    quoteViewItems = getViewItems(quotes.sorted())
                    emitState()
                }
        }
    }

    private fun List<SwapProviderQuote>.sorted(): List<SwapProviderQuote> {
        return this.sortedByDescending { it.amountOut }
    }

    private fun getViewItems(quotes: List<SwapProviderQuote>): List<QuoteViewItem> {
        val fiatAmountIn = getFiatValue(amountIn, rateTokenIn)

        return quotes.map { quote ->
            val fiatAmountOut = getFiatValue(quote.amountOut, rateTokenOut)
            val tokenAmount = App.numberFormatter.formatCoinFull(
                quote.amountOut,
                quote.tokenOut.coin.code,
                quote.tokenOut.decimals
            )
            val priceImpactData = getPriceImpactData(fiatAmountOut?.value, fiatAmountIn?.value)
            QuoteViewItem(
                quote = quote,
                fiatAmount = fiatAmountOut?.getFormattedFull(),
                tokenAmount = tokenAmount,
                priceImpactData = priceImpactData
            )
        }
    }

    private fun getPriceImpactData(amountOut: BigDecimal?, amountIn: BigDecimal?): PriceImpactData? {
        var priceImpact = calculateDiff(amountOut, amountIn)
        val priceImpactAbs = priceImpact?.abs()

        var priceImpactLevel: PriceImpactLevel?

        if (priceImpactAbs == null || priceImpactAbs < normalPriceImpact) {
            priceImpact = null
            priceImpactLevel = null
        } else {
            priceImpactLevel = when {
                priceImpactAbs < warningPriceImpact -> PriceImpactLevel.Normal
                priceImpactAbs < highPriceImpact -> PriceImpactLevel.Warning
                priceImpactAbs < forbiddenPriceImpact -> PriceImpactLevel.High
                else -> PriceImpactLevel.Forbidden
            }
        }

        return priceImpact?.let {
            PriceImpactData(it, priceImpactLevel)
        }
    }

    private fun calculateDiff(amountOut: BigDecimal?, amountIn: BigDecimal?): BigDecimal? {
        if (amountOut == null || amountIn == null || amountIn.compareTo(BigDecimal.ZERO) == 0) return null

        return (amountOut - amountIn)
            .divide(amountIn, RoundingMode.DOWN)
            .times(BigDecimal("100"))
            .setScale(2, RoundingMode.DOWN)
            .stripTrailingZeros()
    }


    override fun createState() = SwapSelectProviderUiState(
        quoteViewItems = quoteViewItems,
        selectedQuote = quote
    )

    private fun getFiatValue(amount: BigDecimal?, rate: BigDecimal?): CurrencyValue? {
        if (amount == null || rate == null) return null

        return CurrencyValue(currency, amount.multiply(rate))
    }

    class Factory(private val quotes: List<SwapProviderQuote>, private val quote: SwapProviderQuote?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapSelectProviderViewModel(quotes, quote) as T
        }
    }
}

data class SwapSelectProviderUiState(
    val quoteViewItems: List<QuoteViewItem>,
    val selectedQuote: SwapProviderQuote?
)

data class QuoteViewItem(
    val quote: SwapProviderQuote,
    val fiatAmount: String?,
    val tokenAmount: String,
    val priceImpactData: PriceImpactData?
)

data class PriceImpactData(
    val priceImpact: BigDecimal,
    val priceImpactLevel: PriceImpactLevel? = null
)
