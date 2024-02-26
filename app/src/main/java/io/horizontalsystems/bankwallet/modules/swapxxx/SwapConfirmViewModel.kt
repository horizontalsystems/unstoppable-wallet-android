package io.horizontalsystems.bankwallet.modules.swapxxx

import android.os.CountDownTimer
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SwapConfirmViewModel(
    private var quote: SwapProviderQuote,
    private val settings: Map<String, Any?>,
) : ViewModelUiState<SwapConfirmUiState>() {
    private var expiresIn = quote.expireAt - System.currentTimeMillis()
    private var expired = false
    private var refreshing = false

    private var expirationTimer: CountDownTimer? = null

    init {
        handleQuote(quote)
    }

    override fun createState() = SwapConfirmUiState(
        quote = quote,
        expiresIn = expiresIn,
        expired = expired,
        refreshing = refreshing
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
            quote.tokenIn,
            quote.tokenOut,
            quote.amountIn,
            settings
        )
    )

    private fun handleQuote(quote: SwapProviderQuote) {
        this.quote = quote

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
            SwapConfirmViewModel(quote, settings)
        }
    }
}


data class SwapConfirmUiState(
    val quote: SwapProviderQuote,
    val expiresIn: Long,
    val expired: Boolean,
    val refreshing: Boolean,
)