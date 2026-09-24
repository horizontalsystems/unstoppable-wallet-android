package io.horizontalsystems.walletkit.modules.crosspay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.collectSafely
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.multiswap.FiatService
import io.horizontalsystems.walletkit.modules.multiswap.SwapPopularTokens
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.math.BigDecimal
import java.math.RoundingMode
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.chain.SendChainSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * The CrossPay tab of the unified send screen: pay an EXACT amount of any provider-supported
 * token to an external address, funded from the wallet's token. The quote here is
 * display-only — the confirmation step commits its own order, so a stale figure can never
 * be funded.
 */
class CrossPayTabViewModel(
    val wallet: Wallet,
    /** The send screen's chain settings (coin control), shared with the other tabs. */
    private val chainSettingsFlow: StateFlow<SendChainSettings?>,
) : ViewModelUiState<CrossPayTabUiState>() {

    val tokenIn = wallet.token
    private val currency = App.currencyManager.baseCurrency
    private val fiatService = FiatService(App.marketKit)
    private val provider = CrossPayManager.resolveProvider()
    private val gson = Gson()

    private var providerReady = false
    private var tokenOut: Token? = null
    private var amountOut: BigDecimal? = null
    private var fiatAmountOut: BigDecimal? = null
    private var fiatAmountInputEnabled = false
    private var address: Address? = null
    private var contactName: String? = null
    private var riskyAddress = false
    private var quote: CrossPayQuoteState? = null
    private var quoteJob: Job? = null
    private val chainPlugin = ChainRegistry[tokenIn.blockchainType]
    private var chainSettings: SendChainSettings? = chainSettingsFlow.value
    private var availableBalance: BigDecimal? = readBalance()

    init {
        fiatService.setCurrency(currency)

        // Picked synchronously, exactly like the swap screen's auto-pick: `uiState` is lazy,
        // so the selector is already filled on the first composed frame instead of flickering
        // in from a coroutine.
        defaultTokenOut()?.let { selectTokenOut(it) }

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
            emitState()
            scheduleQuote()
        }

        // The source balance backs the insufficient-balance step; follow the adapter so it
        // stays current while the wallet syncs, and the settings so a narrowed selection of
        // outputs narrows it too.
        viewModelScope.launch {
            App.adapterManager.getAdapterForWallet<IBalanceAdapter>(wallet)
                ?.balanceUpdatedFlow
                ?.collectSafely {
                    availableBalance = readBalance()
                    emitState()
                }
        }
        viewModelScope.launch {
            chainSettingsFlow.collect {
                chainSettings = it
                availableBalance = readBalance()
                emitState()
            }
        }
    }

    // The chain narrows the balance to the outputs chosen in the settings, if any; otherwise
    // it is the wallet's spendable balance.
    private fun readBalance() = chainPlugin?.sendAvailableBalance(tokenIn, chainSettings)
        ?: App.adapterManager.getAdapterForToken<IBalanceAdapter>(tokenIn)?.balanceData?.available

    // The swap screen's auto-pick, verbatim: the top entry of the context-aware Popular
    // Tokens list (native source → its chain's USDT, else USDT-ETH; token source → its
    // chain's native coin). Keeps CrossPay and swap presenting one behavior; a popular
    // token the provider cannot route just shows "not supported" on the quote row.
    private fun defaultTokenOut(): Token? =
        SwapPopularTokens.build(App.marketKit, tokenIn).firstOrNull()

    override fun createState() = CrossPayTabUiState(
        tokenIn = tokenIn,
        tokenOut = tokenOut,
        amountOut = amountOut,
        fiatAmountOut = fiatAmountOut,
        fiatAmountInputEnabled = fiatAmountInputEnabled,
        currency = currency,
        availableBalance = availableBalance,
        address = address,
        contactName = contactName,
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
        contactName = null
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

    /**
     * A share of the SOURCE balance, expressed in the recipient's token: the entered figure
     * is what the recipient gets, so the share crosses the pair's fiat rates. An estimate by
     * design — the live quote then prices the exact deposit, and the offered shares (100% is
     * not one of them) leave more than enough headroom for the difference.
     */
    fun onEnterAmountPercentage(percentage: Int) {
        val tokenOut = tokenOut ?: return
        val balance = availableBalance ?: return
        if (balance <= BigDecimal.ZERO) return

        val priceIn = App.marketKit.coinPrice(tokenIn.coin.uid, currency.code)
            ?.takeIf { !it.expired }?.value ?: return
        val priceOut = App.marketKit.coinPrice(tokenOut.coin.uid, currency.code)
            ?.takeIf { !it.expired }?.value ?: return

        val amount = balance
            .multiply(BigDecimal(percentage))
            .multiply(priceIn)
            .divide(priceOut.multiply(BigDecimal(100)), tokenOut.decimals, RoundingMode.DOWN)
            .stripTrailingZeros()

        if (amount > BigDecimal.ZERO) {
            fiatService.setAmount(amount)
        }
    }

    fun onSelectAddress(address: Address, risky: Boolean, contactName: String?) {
        this.address = address
        this.riskyAddress = risky
        this.contactName = contactName
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
                        val sellAmount = rate.routes.orEmpty().mapNotNull { it.sellAmount }.minOrNull()
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
                refusalOrNetworkError(e)
            }

            ensureActive()
            quote = newQuote
            emitState()
        }
    }

    // A refused rate is not always a 200: the dev server answers the same
    // { providerErrors } envelope with a 404, which Retrofit surfaces as HttpException
    // before the body is ever parsed. Read the refusal out of the error body so a named
    // reason never degrades into "check connection"; anything unparseable stays Network.
    private fun refusalOrNetworkError(e: Throwable): CrossPayQuoteState.Error {
        if (e is HttpException) {
            val parsed = try {
                gson.fromJson(
                    e.response()?.errorBody()?.string(),
                    UnstoppableAPI.Response.Rate::class.java,
                )
            } catch (parseError: Throwable) {
                null
            }
            val providerErrors = parsed?.providerErrors.orEmpty()
            if (providerErrors.isNotEmpty()) {
                return errorState(providerErrors)
            }
        }
        return CrossPayQuoteState.Error(CrossPayQuoteState.ErrorKind.Network)
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

    override fun onCleared() {
        fiatService.clear()
    }

    class Factory(
        private val wallet: Wallet,
        private val chainSettingsFlow: StateFlow<SendChainSettings?>,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CrossPayTabViewModel(wallet, chainSettingsFlow) as T
        }
    }

    companion object {
        private const val QUOTE_DEBOUNCE_MS = 500L
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
    /** Name of the contact the recipient belongs to, if any. */
    val contactName: String?,
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
