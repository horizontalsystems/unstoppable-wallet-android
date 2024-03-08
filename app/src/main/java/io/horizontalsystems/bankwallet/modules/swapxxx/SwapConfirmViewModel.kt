package cash.p.terminal.modules.swapxxx

import android.os.CountDownTimer
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import cash.p.terminal.core.App
import cash.p.terminal.core.ViewModelUiState
import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.entities.Currency
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SwapConfirmViewModel(
    private var quote: SwapProviderQuote,
    private val settings: Map<String, Any?>,
    private val currencyManager: CurrencyManager,
    private val fiatServiceIn: FiatService,
    private val fiatServiceOut: FiatService,
    private val sendTransactionService: ISendTransactionService?
) : ViewModelUiState<SwapConfirmUiState>() {
    private val currency = currencyManager.baseCurrency
    private val tokenIn = quote.tokenIn
    private val tokenOut = quote.tokenOut
    private val amountIn = quote.amountIn
    private var amountOut = quote.amountOut
    private var swapQuote = quote.swapQuote
    private var fiatAmountIn: BigDecimal? = null
    private var fiatAmountOut: BigDecimal? = null

    private var expiresIn = 10L
    private var expired = false
    private var refreshing = false

    private var expirationTimer: CountDownTimer? = null
    private val swapProvider = quote.provider

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

    private suspend fun reFetchQuote() = SwapProviderQuote(
        provider = swapProvider,
        swapQuote = swapProvider.fetchQuote(tokenIn, tokenOut, amountIn, settings)
    )

    private fun handleQuote(quote: SwapProviderQuote) {
        amountOut = quote.amountOut
        swapQuote = quote.swapQuote
        expiresIn = 10
        expired = false

        fiatServiceOut.setAmount(amountOut)

        emitState()

        runExpirationTimer(
            millisInFuture = expiresIn * 1000,
            onTick = { millisUntilFinished ->
                expiresIn = Math.ceil(millisUntilFinished / 1000.0).toLong()
                emitState()
            },
            onFinish = {
                expired = true
                emitState()
            }
        )

        sendTransactionService?.setSendTransactionData(swapProvider.getSendTransactionData(swapQuote))
    }

    override fun onCleared() {
        expirationTimer?.cancel()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.Default) {
            refreshing = true
            emitState()

            val newQuote = reFetchQuote()
            refreshing = false

            handleQuote(newQuote)
        }
    }

    fun swap() {
        viewModelScope.launch {
            swapProvider.swap(swapQuote)
        }
    }

    fun getSendTransactionService(): ISendTransactionService {
        return sendTransactionService!!
    }

    companion object {
        fun init(quote: SwapProviderQuote, settings: Map<String, Any?>): CreationExtras.() -> SwapConfirmViewModel = {
            val sendTransactionService = SendTransactionServiceFactory.create(quote.tokenIn)

            SwapConfirmViewModel(quote, settings, App.currencyManager, FiatService(App.marketKit), FiatService(App.marketKit), sendTransactionService)
        }
    }
}


data class SwapConfirmUiState(
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