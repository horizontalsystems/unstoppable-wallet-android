package io.horizontalsystems.bankwallet.modules.swapxxx

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.swapxxx.providers.ISwapXxxProvider
import io.horizontalsystems.bankwallet.modules.swapxxx.sendtransaction.ISendTransactionService
import io.horizontalsystems.bankwallet.modules.swapxxx.sendtransaction.SendTransactionServiceFactory
import io.horizontalsystems.bankwallet.modules.swapxxx.sendtransaction.SendTransactionSettings
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SwapConfirmViewModel(
    private val swapProvider: ISwapXxxProvider,
    swapQuote: ISwapQuote,
    private val swapSettings: Map<String, Any?>,
    private val currencyManager: CurrencyManager,
    private val fiatServiceIn: FiatService,
    private val fiatServiceOut: FiatService,
    val sendTransactionService: ISendTransactionService
) : ViewModelUiState<SwapConfirmUiState>() {
    private var sendTransactionSettings: SendTransactionSettings? = null
    private val currency = currencyManager.baseCurrency
    private val tokenIn = swapQuote.tokenIn
    private val tokenOut = swapQuote.tokenOut
    private val amountIn = swapQuote.amountIn
    private var fiatAmountIn: BigDecimal? = null

    private var fiatAmountOut: BigDecimal? = null
    private var expiresIn = 10L
    private var expired = false

    private var refreshing = false
    private var expirationTimer: CountDownTimer? = null

    private var amountOut: BigDecimal? = null

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

        viewModelScope.launch {
            sendTransactionService.stateFlow.collect {
                sendTransactionSettings = it

                fetchFinalQuote(false)
            }
        }

        sendTransactionService.start(viewModelScope)

        fetchFinalQuote(true)
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

    private fun runExpiration() {
        expiresIn = 10
        expired = false

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
    }

    override fun onCleared() {
        expirationTimer?.cancel()
    }

    fun refresh() {
        refreshing = true
        emitState()

        fetchFinalQuote(true)

        refreshing = false
        emitState()
    }

    private fun fetchFinalQuote(runExpiration: Boolean) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val finalQuote = swapProvider.fetchFinalQuote(tokenIn, tokenOut, amountIn, swapSettings, sendTransactionSettings)

                amountOut = finalQuote.amountOut
                emitState()

                fiatServiceOut.setAmount(amountOut)
                sendTransactionService.setSendTransactionData(finalQuote.sendTransactionData)

                if (runExpiration) {
                    runExpiration()
                }
            } catch (t: Throwable) {
                Log.e("AAA", "fetchFinalQuote error", t)
            }
        }
    }


    fun swap() {
        viewModelScope.launch {
            sendTransactionService.sendTransaction()
        }
    }

    companion object {
        fun init(quote: SwapProviderQuote, settings: Map<String, Any?>): CreationExtras.() -> SwapConfirmViewModel = {
            val sendTransactionService = SendTransactionServiceFactory.create(quote.tokenIn)

            SwapConfirmViewModel(
                quote.provider,
                quote.swapQuote,
                settings,
                App.currencyManager,
                FiatService(App.marketKit),
                FiatService(App.marketKit),
                sendTransactionService
            )
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
    val amountOut: BigDecimal?,
    val fiatAmountIn: BigDecimal?,
    val fiatAmountOut: BigDecimal?,
    val currency: Currency,
)