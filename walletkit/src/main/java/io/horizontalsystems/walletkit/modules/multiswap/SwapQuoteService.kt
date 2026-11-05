package io.horizontalsystems.walletkit.modules.multiswap

import android.util.Log
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPath
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFinder
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.ISplitSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.MultiSwapProviderRegistry
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapProviderInfoManager
import io.horizontalsystems.walletkit.modules.multiswap.providers.USwapAssetCatalog
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.math.BigDecimal

class SwapQuoteService(
    // Callers with restricted execution capabilities (e.g. smart accounts that
    // can only broadcast a subset of providers) pass their own set.
    private val allProviders: List<IMultiSwapProvider> = MultiSwapProviderRegistry.allProviders,
    private val providerInfoManager: SwapProviderInfoManager = App.swapProviderInfoManager,
    private val assetCatalog: USwapAssetCatalog = App.uSwapAssetCatalog,
    // Split mode: quotes come from these providers' split endpoints, priced for
    // [splitPercents], and `allProviders` is not consulted. Null quotes single swaps.
    private val splitProviders: List<ISplitSwapProvider>? = null,
    private val pathFinder: SwapPathFinder = SwapPathFinder.default(),
) : Clearable {
    private val tag = "SwapQuoteService"

    private var amountIn: BigDecimal? = null
    private var tokenIn: Token? = null
    private var tokenOut: Token? = null
    private var quoting = false
    private var quotes: List<SwapProviderQuote> = listOf()
    // The venue the user picked on the Routes list, kept while it is still quoted.
    private var preferredVenueId: String? = null
    private var error: Throwable? = null
    // Split mode: the provider's own refusal behind a route-not-found, when it gave one.
    private var splitProviderError: SplitRateException? = null
    private var quote: SwapProviderQuote? = null
    private var splitPercents: List<Int> = SplitSwapRules.percents(SplitSwapRules.PERCENT_DEFAULT)

    val isSplit: Boolean
        get() = splitProviders != null
    private var suggestedPath: SwapPath? = null

    // The pair and provider set whose last quote round came back empty. The amount is
    // deliberately not part of it, so the suggested path stays put while the user edits the
    // amount and leaves only once quotes arrive or the pair changes.
    private var emptyRoundKey: QuoteRoundKey? = null

    private val _stateFlow = MutableStateFlow(
        State(
            amountIn = amountIn,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            quoting = quoting,
            quotes = quotes,
            preferredVenueId = preferredVenueId,
            quote = quote,
            error = error,
            splitPercents = splitPercents,
            splitProviderError = splitProviderError,
            suggestedPath = suggestedPath,
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    private var coroutineScope = CoroutineScope(Dispatchers.Default)
    private var quotingJob: Job? = null
    // Cancelling a quotation is cooperative: one whose fetch has already returned still runs its
    // publication. The generation it was started under tells it that it has been superseded, and
    // the lock keeps that check and its writes from interleaving with the next quotation's reset.
    private val publishLock = Any()
    private var quotationGeneration = 0
    private var startProvidersJob: Job? = null

    fun start() {
        // A session per swap screen: the quotes compared here are linked to the swap made from it.
        startNewSession()

        // Read before the syncs are launched, so a list landing before the collector subscribes
        // still reads as a change rather than arriving as the collector's own first value.
        val startingCatalogVersion = assetCatalog.syncFlow.value
        val startingSplitProviders = providerInfoManager.splitProvidersFlow.value

        val initialQuotation = launchGuarded("initial quotation") {
            startProviders()
            runQuotation()
        }

        launchGuarded("catalog updates") {
            // Waiting for the first quotation: re-running it before the providers have started
            // would quote against a catalog that supports nothing yet and surface "no supported
            // provider" for as long as the startup takes.
            initialQuotation.join()

            // A catalog that lands can turn an unsupported pair into a quotable one, or name a
            // token the last round could not. Re-run only for a round that found nothing: a
            // sync that changes nothing visible must not refresh the quotes under the user.
            assetCatalog.syncFlow
                .dropWhile { it == startingCatalogVersion }
                .collect {
                    try {
                        if (error != null) runQuotation()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        // Caught per emission, not just at the launch: the collector is the only
                        // path back from a catalog change, so it has to outlive a failed run.
                        Log.e(tag, "error on re-quoting after a catalog change", e)
                    }
                }
        }

        if (splitProviders != null) {
            // Which venues may split is the server's call, made by the provider sync: a change
            // adds or removes split quotes without touching the token selection.
            launchGuarded("split provider updates") {
                initialQuotation.join()
                providerInfoManager.splitProvidersFlow
                    .dropWhile { it == startingSplitProviders }
                    .collect {
                        try {
                            runQuotation()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            Log.e(tag, "error on re-quoting after a split provider change", e)
                        }
                    }
            }
        }
    }

    /** A new `X-Session-Id` for the next swap: on open, and once a swap has been sent. */
    fun startNewSession() {
        val providers: List<IMultiSwapProvider> = splitProviders ?: allProviders
        providers.forEach { it.startSession() }
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    /**
     * The service scope is not a supervisor, so an exception escaping any one of these coroutines
     * would cancel the others with it — the sync collector included.
     */
    private fun launchGuarded(what: String, block: suspend CoroutineScope.() -> Unit) =
        coroutineScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(tag, "error on $what", e)
            }
        }

    fun restart(onRestart: () -> Unit) {
        startProvidersJob?.cancel()
        startProvidersJob = coroutineScope.launch {
            startProviders()
            onRestart()
        }
    }

    private suspend fun CoroutineScope.startProviders() {
        (splitProviders ?: allProviders).map { provider ->
            async {
                try {
                    provider.start()
                } catch (e: Throwable) {
                    Log.e(tag, "error on starting ${provider.id}", e)
                }
            }
        }.awaitAll()
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                amountIn = amountIn,
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                quoting = quoting,
                quotes = quotes,
                preferredVenueId = preferredVenueId,
                quote = quote,
                error = error,
                splitPercents = splitPercents,
                splitProviderError = splitProviderError,
                suggestedPath = suggestedPath,
            )
        }
    }

    private fun runQuotation(silent: Boolean = false) {
        quotingJob?.cancel()
        val generation = synchronized(publishLock) {
            quoting = false
            quotes = listOf()
            quote = null
            error = null
            splitProviderError = null
            ++quotationGeneration
        }

        if (!silent) {
            emitState()
        }

        val tokenIn = tokenIn
        val tokenOut = tokenOut
        val amountIn = amountIn

        if (tokenIn != null && tokenOut != null) {
            // In split mode the split endpoints are asked; which venues answer is the server's
            // call, and the provider keeps only the ones the directory flags as split-capable.
            val candidates: List<IMultiSwapProvider> = splitProviders ?: allProviders
            val supportedProviders = candidates.filter { provider ->
                // Checked here, in the one place every provider passes through: a pair no
                // provider can ask for shows as unsupported before any amount is typed.
                provider.supports(tokenIn, tokenOut)
            }

            val roundKey = QuoteRoundKey(tokenIn, tokenOut, supportedProviders.map { it.id }.toSet())

            if (emptyRoundKey != roundKey) {
                // A changed pair or an untried set of providers has not failed at anything yet.
                if (clearSuggestedPath(generation)) emitState()
            }

            if (supportedProviders.isEmpty()) {
                error = NoSupportedSwapProvider()
                emitState()

                // The lookup reads the coin database, so it runs off the caller's thread like the
                // quote round does. The previous suggestion for this pair stays visible until the
                // fresh one replaces it.
                quotingJob = coroutineScope.launch {
                    val path = findSuggestedPath(tokenIn, tokenOut)

                    val published = synchronized(publishLock) {
                        if (generation != quotationGeneration) return@synchronized false

                        emptyRoundKey = roundKey
                        suggestedPath = path
                        true
                    }
                    if (published) emitState()
                }
            } else if (amountIn != null && amountIn > BigDecimal.ZERO) {
                if (!silent) {
                    quoting = true
                    emitState()
                }

                val percents = splitPercents
                quotingJob = coroutineScope.launch {
                    val results = fetchQuotes(supportedProviders, tokenIn, tokenOut, amountIn, percents)
                    val path = if (results.quotes.isEmpty()) findSuggestedPath(tokenIn, tokenOut) else null

                    val published = synchronized(publishLock) {
                        if (generation != quotationGeneration) return@synchronized false

                        quotes = results.quotes
                        // Kept apart from `error`, which stays the generic route-not-found the swap
                        // button reports: the refusal only refines the split's own alert.
                        splitProviderError = results.failures
                            .filterIsInstance<SplitRateException>()
                            .let { refusals -> refusals.firstOrNull { it.belowMinimum } ?: refusals.firstOrNull() }

                        val preferred = preferredVenueId
                        if (preferred != null && quotes.none { it.venue.id == preferred }) {
                            preferredVenueId = null
                        }

                        if (quotes.isEmpty()) {
                            error = SwapRouteNotFound()
                            emptyRoundKey = roundKey
                            suggestedPath = path
                        } else {
                            emptyRoundKey = null
                            suggestedPath = null
                            quote = preferredVenueId
                                ?.let { venueId -> quotes.find { it.venue.id == venueId } }
                                ?: quotes.firstOrNull()
                        }

                        quoting = false
                        true
                    }
                    if (published) emitState()
                }
            }
        } else if (clearSuggestedPath(generation)) {
            emitState()
        }
    }

    /**
     * Under the same generation check as a quote publication: the provider-support work above
     * runs unlocked, so a quotation overtaken meanwhile must not clear what the newer one found.
     */
    private fun clearSuggestedPath(generation: Int): Boolean = synchronized(publishLock) {
        if (generation != quotationGeneration) return@synchronized false

        emptyRoundKey = null
        suggestedPath = null
        true
    }

    /**
     * Null in split mode: the finder reasons about the regular provider set, and a split swap has
     * no first-leg screen to hand the intermediate token to.
     */
    private fun findSuggestedPath(tokenIn: Token, tokenOut: Token): SwapPath? {
        if (splitProviders != null) return null

        return try {
            pathFinder.find(tokenIn, tokenOut, allProviders)
        } catch (e: Throwable) {
            Log.e(tag, "error on finding a suggested path", e)
            null
        }
    }

    private suspend fun fetchQuotes(
        supportedProviders: List<IMultiSwapProvider>,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        splitPercents: List<Int>,
    ): QuoteResults = coroutineScope {
        val results = supportedProviders
            .map { provider ->
                async {
                    try {
                        // One request fans out to every venue server-side, so the round takes as
                        // long as the slowest venue the server waits for.
                        withTimeout(QUOTE_TIMEOUT_MS) {
                            val swapQuotes = if (isSplit && provider is ISplitSwapProvider) {
                                provider.fetchSplitQuotes(tokenIn, tokenOut, amountIn, splitPercents)
                            } else {
                                provider.fetchQuotes(tokenIn, tokenOut, amountIn)
                            }
                            Result.success(swapQuotes.map { SwapProviderQuote(provider = provider, swapQuote = it) })
                        }
                    } catch (e: Throwable) {
                        Log.e(tag, "fetchQuoteError: ${provider.id}", e)
                        Result.failure(e)
                    }
                }
            }
            .awaitAll()

        QuoteResults(
            quotes = results.flatMap { it.getOrNull().orEmpty() }.sortedByDescending { it.amountOut },
            failures = results.mapNotNull { it.exceptionOrNull() },
        )
    }

    fun setAmount(v: BigDecimal?) {
        if (amountIn == v) return

        amountIn = v
        preferredVenueId = null

        runQuotation()
    }

    fun setTokenIn(token: Token) {
        if (tokenIn == token) return

        preferredVenueId = null
        if (tokenOut == token) {
            // selected token is already on the other side, swap places instead of clearing it
            tokenOut = tokenIn
        }
        tokenIn = token

        runQuotation()
    }

    fun setTokenOut(token: Token) {
        if (tokenOut == token) return

        preferredVenueId = null
        if (tokenIn == token) {
            // selected token is already on the other side, swap places instead of clearing it
            tokenIn = tokenOut
        }
        tokenOut = token

        runQuotation()
    }

    /** Split mode: re-prices the outputs for new shares. The selected venue is kept. */
    fun setSplitPercents(percents: List<Int>) {
        if (splitPercents == percents) return
        SplitSwapRules.requireValid(percents)

        splitPercents = percents

        runQuotation()
    }

    fun switchPairs() {
        val tmpTokenIn = tokenIn

        tokenIn = tokenOut
        tokenOut = tmpTokenIn

        amountIn = quote?.amountOut

        runQuotation()
    }

    fun selectQuote(quote: SwapProviderQuote) {
        preferredVenueId = quote.venue.id
        this.quote = quote

        emitState()
    }

    fun reQuote() {
        runQuotation(silent = true)
    }

    fun onActionStarted(quote: SwapProviderQuote?) {
        preferredVenueId = quote?.venue?.id
    }

    fun onActionCompleted() {
        runQuotation()
    }

    data class State(
        val amountIn: BigDecimal?,
        val tokenIn: Token?,
        val tokenOut: Token?,
        val quoting: Boolean,
        val quotes: List<SwapProviderQuote>,
        val preferredVenueId: String?,
        val quote: SwapProviderQuote?,
        val error: Throwable?,
        // The shares the split quotes were priced for; meaningless for single swaps.
        val splitPercents: List<Int>,
        // Split mode: why a provider refused to price these shares, when it said.
        val splitProviderError: SplitRateException? = null,
        val suggestedPath: SwapPath? = null,
    )

    private class QuoteResults(
        val quotes: List<SwapProviderQuote>,
        val failures: List<Throwable>,
    )

    private data class QuoteRoundKey(
        val tokenIn: Token,
        val tokenOut: Token,
        val providerIds: Set<String>,
    )

    companion object {
        private const val QUOTE_TIMEOUT_MS = 15_000L
    }
}
