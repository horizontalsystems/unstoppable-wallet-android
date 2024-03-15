package io.horizontalsystems.bankwallet.modules.swapxxx

import android.os.CountDownTimer
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SwapConfirmViewModel(
    private var quote: SwapProviderQuote,
    private val settings: Map<String, Any?>,
    private val currencyManager: CurrencyManager,
    private val fiatServiceIn: FiatService,
    private val fiatServiceOut: FiatService
) : ViewModelUiState<SwapConfirmUiState>() {
    private val currency = currencyManager.baseCurrency
    private val tokenIn = quote.tokenIn
    private val tokenOut = quote.tokenOut
    private val amountIn = quote.amountIn
    private var amountOut = quote.amountOut
    private var fiatAmountIn: BigDecimal? = null
    private var fiatAmountOut: BigDecimal? = null

    private var expiresIn = quote.expireAt - System.currentTimeMillis()
    private var expired = false
    private var refreshing = false

    private var expirationTimer: CountDownTimer? = null

    init {
        fiatServiceIn.setCurrency(currency)
        fiatServiceIn.setToken(tokenIn)
        fiatServiceIn.setAmount(amountIn)

        fiatServiceOut.setCurrency(currency)
        fiatServiceOut.setToken(tokenOut)
        fiatServiceOut.setAmount(amountOut)

        viewModelScope.launch {
            fiatServiceIn.stateFlow.collect {
                fiatAmountIn = it.fiatAmount
                emitState()
            }
        }

        viewModelScope.launch {
            fiatServiceOut.stateFlow.collect {
                fiatAmountOut = it.fiatAmount
                emitState()
            }
        }

        handleQuote(quote)
    }

    override fun createState() = SwapConfirmUiState(
        quote = quote,
        expiresIn = expiresIn,
        expired = expired,
        refreshing = refreshing,
        tokenIn = tokenIn,
        tokenOut = tokenOut,
        amountIn = amountIn,
        amountOut = amountOut,
        fiatAmountIn = fiatAmountIn,
        fiatAmountOut = fiatAmountOut,
        currency = currency
    )

    private fun runExpirationTimer(millisInFuture: Long, onTick: (Long) -> Unit, onFinish: () -> Unit) {
        viewModelScope.launch {
            expirationTimer?.cancel()
            expirationTimer = object : CountDownTimer(millisInFuture, 1000) {
                override fun onTick(millisUntilFinished: Long) = onTick.invoke(millisUntilFinished)
                override fun onFinish() = onFinish.invoke()
            }
            expirationTimer?.start()
        }
    }

    private suspend fun reFetchQuote(quote: SwapProviderQuote) = SwapProviderQuote(
        provider = quote.provider,
        swapQuote = quote.provider.fetchQuote(
            tokenIn,
            tokenOut,
            amountIn,
            settings
        )
    )

    private fun handleQuote(quote: SwapProviderQuote) {
        this.quote = quote
        amountOut = quote.amountOut
        fiatServiceOut.setAmount(amountOut)

        expiresIn = quote.expireAt - System.currentTimeMillis()
        expired = false
        emitState()

        runExpirationTimer(
            millisInFuture = expiresIn,
            onTick = { millisUntilFinished ->
                expiresIn = millisUntilFinished
                emitState()
            },
            onFinish = {
                expired = true
                emitState()
            }
        )
    }

    override fun onCleared() {
        expirationTimer?.cancel()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.Default) {
            refreshing = true
            emitState()

            val newQuote = reFetchQuote(quote)
            refreshing = false

            handleQuote(newQuote)
        }
    }

    fun swap() {
        viewModelScope.launch {
            quote.provider.swap(quote.swapQuote)
        }
    }

    companion object {
        fun init(quote: SwapProviderQuote, settings: Map<String, Any?>): CreationExtras.() -> SwapConfirmViewModel = {
            SwapConfirmViewModel(quote, settings, App.currencyManager, FiatService(App.marketKit), FiatService(App.marketKit))
        }
    }
}


data class SwapConfirmUiState(
    val quote: SwapProviderQuote,
    val expiresIn: Long,
    val expired: Boolean,
    val refreshing: Boolean,
    val tokenIn: Token,
    val tokenOut: Token,
    val amountIn: BigDecimal,
    val amountOut: BigDecimal,
    val fiatAmountIn: BigDecimal?,
    val fiatAmountOut: BigDecimal?,
    val currency: Currency,
)