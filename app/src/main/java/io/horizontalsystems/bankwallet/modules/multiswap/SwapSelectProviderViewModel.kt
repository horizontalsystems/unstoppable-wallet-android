package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import java.math.BigDecimal

class SwapSelectProviderViewModel(private val quotes: List<SwapProviderQuote>) : ViewModelUiState<SwapSelectProviderUiState>() {
    private val currencyManager = App.currencyManager
    private val marketKit = App.marketKit

    private val currency = currencyManager.baseCurrency
    private var token: Token = quotes.first().tokenOut
    private var rate: BigDecimal? = marketKit.coinPrice(token.coin.uid, currency.code)?.value
    private var quoteViewItems = getViewItems(quotes.sorted())

    init {
        viewModelScope.launch {
            marketKit.coinPriceObservable("swap-providers", token.coin.uid, currency.code)
                .asFlow()
                .collect {
                    rate = it.value
                    quoteViewItems = getViewItems(quotes.sorted())
                    emitState()
                }
        }
    }

    private fun List<SwapProviderQuote>.sorted(): List<SwapProviderQuote> {
        return this.sortedByDescending { it.amountOut }
    }

    private fun getViewItems(quotes: List<SwapProviderQuote>) =
        quotes.map { quote ->
            val fiatAmount = getFiatValue(quote.amountOut)?.getFormattedFull()
            val tokenAmount = App.numberFormatter.formatCoinFull(
                quote.amountOut,
                quote.tokenOut.coin.code,
                quote.tokenOut.decimals
            )
            QuoteViewItem(
                quote,
                fiatAmount,
                tokenAmount,
                getPriceImpactData(quote)
                )
        }

    private fun getPriceImpactData(quote: SwapProviderQuote): PriceImpactData? {
        return PriceImpactData(20.toBigDecimal(), PriceImpactLevel.Warning) //todo implement price impact calculation
    }

    override fun createState() = SwapSelectProviderUiState(
        quoteViewItems = quoteViewItems
    )

    private fun getFiatValue(amount: BigDecimal?): CurrencyValue? {
        return amount?.let {
            rate?.multiply(it)
        }?.let { fiatBalance ->
            CurrencyValue(currency, fiatBalance)
        }
    }

    class Factory(private val quotes: List<SwapProviderQuote>) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapSelectProviderViewModel(quotes) as T
        }
    }
}

data class SwapSelectProviderUiState(val quoteViewItems: List<QuoteViewItem>)

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
