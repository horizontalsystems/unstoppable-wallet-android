package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import java.math.BigDecimal

class SwapSelectProviderViewModel(
    private val quotes: List<SwapProviderQuote>,
    private val quote: SwapProviderQuote?
) : ViewModelUiState<SwapSelectProviderUiState>() {
    private val currencyManager = App.currencyManager
    private val marketKit = App.marketKit

    private val currency = currencyManager.baseCurrency
    private var tokenIn = quotes.first().tokenIn
    private var amountIn = quotes.first().amountIn
    private var tokenOut = quotes.first().tokenOut
    private var rateTokenIn: BigDecimal? = marketKit.coinPrice(tokenIn.coin.uid, currency.code)?.value
    private var rateTokenOut: BigDecimal? = marketKit.coinPrice(tokenOut.coin.uid, currency.code)?.value
    private var quoteViewItems = getViewItems(quotes.sorted())
    private var sortType = ProviderSortType.BestPrice

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
            val priceImpactData = PriceImpactCalculator.getPriceImpactData(
                fiatAmountOut?.value,
                fiatAmountIn?.value,
                PriceImpactLevel.Warning
            )
            QuoteViewItem(
                quote = quote,
                fiatAmount = fiatAmountOut?.getFormattedFull(),
                tokenAmount = tokenAmount,
                priceImpactData = priceImpactData
            )
        }
    }

    override fun createState() = SwapSelectProviderUiState(
        quoteViewItems = quoteViewItems,
        selectedQuote = quote,
        sortType = sortType,
    )

    private fun getFiatValue(amount: BigDecimal?, rate: BigDecimal?): CurrencyValue? {
        if (amount == null || rate == null) return null

        return CurrencyValue(currency, amount.multiply(rate))
    }

    fun setSortType(sortType: ProviderSortType) {
        this.sortType = sortType
        quoteViewItems = getViewItems(quotes.sorted())
        emitState()
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
    val selectedQuote: SwapProviderQuote?,
    val sortType: ProviderSortType,
)

data class QuoteViewItem(
    val quote: SwapProviderQuote,
    val fiatAmount: String?,
    val tokenAmount: String,
    val priceImpactData: PriceImpactData?
)

enum class ProviderSortType(val title: Int) {
    BestPrice(R.string.SwapSort_BestPrice),
    BestTime(R.string.SwapSort_BestTime),
    Recommended(R.string.SwapSort_Recommended);
}