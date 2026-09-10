package io.horizontalsystems.walletkit.modules.crosspay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.collectSafely
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
 * The CrossPay tab of the unified send screen: pay an EXACT amount of any provider-supported
 * token to an external address, funded from the wallet's token. The quote here is
 * display-only — the confirmation step commits its own order, so a stale figure can never
 * be funded.
 */
class CrossPayTabViewModel(
    val wallet: Wallet,
) : ViewModelUiState<CrossPayTabUiState>() {

    val tokenIn = wallet.token
    private val currency = App.currencyManager.baseCurrency
    private val fiatService = FiatService(App.marketKit)
    private val provider = CrossPayManager.resolveProvider()

    private var providerReady = false
    private var tokenOut: Token? = null
    private var amountOut: BigDecimal? = null
    private var fiatAmountOut: BigDecimal? = null
    private var fiatAmountInputEnabled = false
    private var address: Address? = null
    private var riskyAddress = false
    private var quote: CrossPayQuoteState? = null
    private var quoteJob: Job? = null
    private var availableBalance: BigDecimal? = readBalance()

    init {
        fiatService.setCurrency(currency)

        viewModelScope.launch {
            fiatService.stateFlow.collect {
                val amountChanged = amountOut != it.amount
                amountOut = it.amount
                fiatAmountOut = it.fiatAmount
                fiatAmountInputEnabled = it.coinPrice != null && !it.coinPrice.expired
                emitState()
                if (amountChanged) {
                    scheduleQuote()
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Populates the provider's token map (server-side sync, cached); until it
                // lands any pending quote request waits via the re-schedule below.
                provider?.start()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Quoting reports the failure per attempt; the tab itself stays usable.
            }
            providerReady = true
            if (tokenOut == null) {
                defaultTokenOut()?.let { selectTokenOut(it) }
            }
            emitState()
            scheduleQuote()
        }

        // The source balance backs the insufficient-balance step; follow the adapter so it
        // stays current while the wallet syncs.
        viewModelScope.launch {
            App.adapterManager.getAdapterForWallet<IBalanceAdapter>(wallet)
                ?.balanceUpdatedFlow
                ?.collectSafely {
                    availableBalance = readBalance()
                    emitState()
                }
        }
    }

    private fun readBalance() = App.adapterManager
        .getAdapterForToken<IBalanceAdapter>(tokenIn)
        ?.balanceData?.available

    // The stablecoin people actually get paid in, else the biggest supported coin. Only a
    // convenience preselection — the user can always change it.
    private fun defaultTokenOut(): Token? {
        val supported = provider?.supportedTokensOut(tokenIn) ?: return null
        return supported.firstOrNull { it.coin.code == "USDT" }
            ?: supported.firstOrNull { it.coin.code == "USDC" }
            ?: supported.minByOrNull { it.coin.marketCapRank ?: Int.MAX_VALUE }
    }

    override fun createState() = CrossPayTabUiState(
        tokenIn = tokenIn,
        tokenOut = tokenOut,
        amountOut = amountOut,
        fiatAmountOut = fiatAmountOut,
        fiatAmountInputEnabled = fiatAmountInputEnabled,
        currency = currency,
        availableBalance = availableBalance,
        address = address,
        riskyAddress = riskyAddress,
        quote = quote,
        step = step(),
    )

    private fun step(): CrossPayStep {
        val amountOut = amountOut
        if (tokenOut == null || amountOut == null || amountOut <= BigDecimal.ZERO) {
            return CrossPayStep.EnterAmount
        }

        // The shortfall is against the DEPOSIT the quote asks for, not the entered amount —
        // those are different tokens.
        (quote as? CrossPayQuoteState.Success)?.let { quote ->
            val balance = availableBalance
            if (balance != null && quote.sellAmount > balance) {
                return CrossPayStep.InsufficientBalance
            }
        }

        (quote as? CrossPayQuoteState.Error)?.let {
            return CrossPayStep.QuoteError(it)
        }

        if (address == null) {
            return CrossPayStep.EnterAddress
        }

        return if (quote is CrossPayQuoteState.Success) {
            CrossPayStep.Proceed
        } else {
            // Loading or not yet requested — everything else is filled, wait for the price.
            CrossPayStep.Quoting
        }
    }

    private fun selectTokenOut(token: Token) {
        tokenOut = token
        fiatService.setToken(token)
        // The entered amount means "of the previous token", and the address is validated
        // against the previous chain — neither may survive the switch.
        fiatService.setAmount(null)
        address = null
        riskyAddress = false
        quote = null
    }

    fun onSelectTokenOut(token: Token) {
        if (tokenOut == token) return
        selectTokenOut(token)
        emitState()
    }

    fun onEnterAmount(amount: BigDecimal?) = fiatService.setAmount(amount)

    fun onEnterFiatAmount(amount: BigDecimal?) = fiatService.setFiatAmount(amount)

    fun onSelectQuickAmount(amount: Int) = fiatService.setAmount(BigDecimal(amount))

    fun onSelectAddress(address: Address, risky: Boolean) {
        this.address = address
        this.riskyAddress = risky
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
            return CrossPayTabViewModel(wallet) as T
        }
    }

    companion object {
        private const val QUOTE_DEBOUNCE_MS = 500L

        val QUICK_AMOUNTS = listOf(100, 200, 500, 1000)
    }
}

data class CrossPayTabUiState(
    val tokenIn: Token,
    val tokenOut: Token?,
    val amountOut: BigDecimal?,
    val fiatAmountOut: BigDecimal?,
    val fiatAmountInputEnabled: Boolean,
    val currency: Currency,
    val availableBalance: BigDecimal?,
    val address: Address?,
    val riskyAddress: Boolean,
    val quote: CrossPayQuoteState?,
    val step: CrossPayStep,
)

sealed class CrossPayStep {
    data object EnterAmount : CrossPayStep()
    data object EnterAddress : CrossPayStep()
    data object InsufficientBalance : CrossPayStep()
    data class QuoteError(val error: CrossPayQuoteState.Error) : CrossPayStep()
    data object Quoting : CrossPayStep()
    data object Proceed : CrossPayStep()
}

sealed interface CrossPayQuoteState {
    data object Loading : CrossPayQuoteState

    // What the sender pays in the wallet's token for the entered exact output.
    data class Success(val sellAmount: BigDecimal) : CrossPayQuoteState

    // The min/max figures are in the DESTINATION token — that is the amount field the user
    // can act on.
    data class Error(val kind: ErrorKind, val amount: BigDecimal? = null) : CrossPayQuoteState

    enum class ErrorKind { NotSupported, NoRoute, BelowMinimum, AboveMaximum, Network }
}
