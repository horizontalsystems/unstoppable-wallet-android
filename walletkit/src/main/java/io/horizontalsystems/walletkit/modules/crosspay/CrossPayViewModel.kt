package io.horizontalsystems.walletkit.modules.crosspay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.multiswap.FiatService
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * The CrossPay entry screen: the recipient's token, address and EXACT amount, priced live
 * against the exact-output rail. The quote here is display-only — the confirmation step
 * commits its own order, so a stale figure can never be funded.
 */
class CrossPayViewModel(
    private val wallet: Wallet,
) : ViewModelUiState<CrossPayUiState>() {

    val tokenIn = wallet.token
    private val currency = App.currencyManager.baseCurrency
    private val fiatService = FiatService(App.marketKit)

    private val provider = CrossPayManager.resolveProvider()

    private var providerReady = false
    private var tokenOut: Token? = null
    private var amountOut: BigDecimal? = null
    private var fiatAmountOut: BigDecimal? = null
    private var recipient: Address? = null
    private var quote: CrossPayQuoteState? = null
    private var quoteJob: Job? = null

    private val availableBalance = App.adapterManager
        .getAdapterForToken<IBalanceAdapter>(tokenIn)
        ?.balanceData?.available

    init {
        fiatService.setCurrency(currency)

        viewModelScope.launch {
            fiatService.stateFlow.collect {
                val amountChanged = amountOut != it.amount
                amountOut = it.amount
                fiatAmountOut = it.fiatAmount
                emitState()
                if (amountChanged) {
                    scheduleQuote()
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Populates the provider's token map (cached server-side sync); until it
                // lands any pending quote request waits via the re-schedule below.
                provider?.start()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Quoting reports the failure per attempt; the screen itself stays usable.
            }
            providerReady = true
            scheduleQuote()
        }
    }

    override fun createState() = CrossPayUiState(
        tokenIn = tokenIn,
        tokenOut = tokenOut,
        amountOut = amountOut,
        fiatAmountOut = fiatAmountOut,
        currency = currency,
        availableBalance = availableBalance,
        quote = quote,
        recipient = recipient,
        canReview = recipient != null && quote is CrossPayQuoteState.Success,
    )

    fun onSelectTokenOut(token: Token) {
        if (tokenOut == token) return

        tokenOut = token
        fiatService.setToken(token)
        // The entered amount means "of the previous token" — carrying it over would quote a
        // different payment than the user typed.
        fiatService.setAmount(null)
        recipient = null
        quote = null
        emitState()
    }

    fun onEnterAmount(amount: BigDecimal?) = fiatService.setAmount(amount)

    fun onEnterFiatAmount(amount: BigDecimal?) = fiatService.setFiatAmount(amount)

    fun onEnterRecipient(address: Address?) {
        recipient = address
        emitState()
    }

    private fun scheduleQuote() {
        quoteJob?.cancel()

        val tokenOut = tokenOut
        val amountOut = amountOut

        if (tokenOut == null || amountOut == null || amountOut <= BigDecimal.ZERO) {
            quote = null
            emitState()
            return
        }

        quote = CrossPayQuoteState.Loading
        emitState()

        quoteJob = viewModelScope.launch(Dispatchers.IO) {
            delay(QUOTE_DEBOUNCE_MS)

            // The provider sync re-schedules on completion; staying in Loading until then
            // beats flashing an error the user cannot act on.
            if (!providerReady) return@launch

            val newQuote = try {
                val provider = provider
                when {
                    provider == null || !provider.supports(tokenIn, tokenOut) ->
                        CrossPayQuoteState.Error(CrossPayQuoteState.ErrorKind.NotSupported)

                    else -> {
                        val rate = provider.exactOutputRate(
                            tokenIn = tokenIn,
                            tokenOut = tokenOut,
                            amountOut = amountOut,
                            slippage = IMultiSwapProvider.DEFAULT_SLIPPAGE,
                        )
                        val sellAmount = rate.routes.mapNotNull { it.sellAmount }.minOrNull()
                        if (sellAmount != null) {
                            CrossPayQuoteState.Success(sellAmount)
                        } else {
                            errorState(rate.providerErrors.orEmpty())
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                CrossPayQuoteState.Error(CrossPayQuoteState.ErrorKind.Network)
            }

            ensureActive()
            quote = newQuote
            emitState()
        }
    }

    private fun errorState(providerErrors: List<UnstoppableAPI.Response.ProviderError>): CrossPayQuoteState.Error {
        val outOfRange = providerErrors.filter { it.errorCode == "amountOutOfRange" }

        outOfRange.mapNotNull { it.minimumAmount }.minOrNull()?.let {
            return CrossPayQuoteState.Error(CrossPayQuoteState.ErrorKind.BelowMinimum, it)
        }

        outOfRange.mapNotNull { it.maximumAmount }.maxOrNull()?.let {
            return CrossPayQuoteState.Error(CrossPayQuoteState.ErrorKind.AboveMaximum, it)
        }

        return CrossPayQuoteState.Error(CrossPayQuoteState.ErrorKind.NoRoute)
    }

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CrossPayViewModel(wallet) as T
        }
    }

    companion object {
        private const val QUOTE_DEBOUNCE_MS = 500L
    }
}

data class CrossPayUiState(
    val tokenIn: Token,
    val tokenOut: Token?,
    val amountOut: BigDecimal?,
    val fiatAmountOut: BigDecimal?,
    val currency: Currency,
    val availableBalance: BigDecimal?,
    val quote: CrossPayQuoteState?,
    val recipient: Address?,
    val canReview: Boolean,
)

sealed interface CrossPayQuoteState {
    data object Loading : CrossPayQuoteState

    // What the sender pays in [CrossPayUiState.tokenIn] for the entered exact output.
    data class Success(val sellAmount: BigDecimal) : CrossPayQuoteState

    // The min/max figures are in the DESTINATION token — that is the amount field the user
    // can act on.
    data class Error(val kind: ErrorKind, val amount: BigDecimal? = null) : CrossPayQuoteState

    enum class ErrorKind { NotSupported, NoRoute, BelowMinimum, AboveMaximum, Network }
}
