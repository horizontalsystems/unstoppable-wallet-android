package cash.p.terminal.modules.multiswap

import androidx.annotation.VisibleForTesting
import cash.p.terminal.core.isNative
import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token

import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.math.BigDecimal

class MultiSwapRouteResolver(
    private val marketKit: MarketKitWrapper,
    private val dispatcherProvider: DispatcherProvider,
) {
    companion object {
        private const val TIMEOUT_MS = 5000L
        private val TON_COMMISSION_RESERVE = BigDecimal.ONE
        private val DEFAULT_COMMISSION_RESERVE = BigDecimal("0.01")
    }

    suspend fun findRoute(
        providers: List<IMultiSwapProvider>,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>,
    ): MultiSwapRoute? = withContext(dispatcherProvider.io) {
        if (tokenIn.type.isNative && tokenOut.type.isNative) return@withContext null

        val candidates = buildCandidateIntermediates(tokenIn, tokenOut)
        if (candidates.isEmpty()) return@withContext null

        val validRoutes = findValidRoutes(providers, candidates, tokenIn, tokenOut)
        if (validRoutes.isEmpty()) return@withContext null

        validRoutes
            .mapNotNull { validRoute ->
                buildRoute(
                    validRoute.intermediate,
                    validRoute.leg1Providers,
                    validRoute.leg2Providers,
                    tokenIn, tokenOut, amountIn, settings,
                )
            }
            .maxByOrNull { it.selectedLeg2Quote.amountOut }
    }

    @VisibleForTesting
    internal fun buildCandidateIntermediates(tokenIn: Token, tokenOut: Token): List<Token> {
        val intermediateA = if (!tokenIn.type.isNative) marketKit.nativeToken(tokenIn.blockchainType) else null
        val intermediateB = if (!tokenOut.type.isNative) marketKit.nativeToken(tokenOut.blockchainType) else null

        return buildList {
            if (intermediateA != null && intermediateA != tokenOut) add(intermediateA)
            if (intermediateB != null && intermediateB != intermediateA && intermediateB != tokenIn) add(intermediateB)
        }
    }

    private data class ValidatedRoute(
        val intermediate: Token,
        val leg1Providers: List<IMultiSwapProvider>,
        val leg2Providers: List<IMultiSwapProvider>,
    )

    private suspend fun findValidRoutes(
        providers: List<IMultiSwapProvider>,
        candidates: List<Token>,
        tokenIn: Token,
        tokenOut: Token,
    ): List<ValidatedRoute> = coroutineScope {
        candidates.map { intermediate ->
            async {
                val legs = listOf(
                    async { filterSupported(providers, tokenIn, intermediate) },
                    async { filterSupported(providers, intermediate, tokenOut) },
                )
                val (leg1Providers, leg2Providers) = legs.awaitAll()
                if (leg1Providers.isNotEmpty() && leg2Providers.isNotEmpty()) {
                    ValidatedRoute(intermediate, leg1Providers, leg2Providers)
                } else null
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun filterSupported(
        providers: List<IMultiSwapProvider>,
        tokenFrom: Token,
        tokenTo: Token,
    ): List<IMultiSwapProvider> = coroutineScope {
        providers.map { provider ->
            async {
                try {
                    withTimeoutOrNull(TIMEOUT_MS) {
                        if (provider.supports(tokenFrom, tokenTo)) provider else null
                    }
                } catch (e: Throwable) {
                    Timber.d(e, "supports error: ${provider.id}")
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun buildRoute(
        intermediate: Token,
        leg1Providers: List<IMultiSwapProvider>,
        leg2Providers: List<IMultiSwapProvider>,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>,
    ): MultiSwapRoute? = coroutineScope {
        val leg1Quotes = fetchQuotes(leg1Providers, tokenIn, intermediate, amountIn, settings)
        if (leg1Quotes.isEmpty()) return@coroutineScope null

        val bestLeg1 = leg1Quotes.first()
        val commissionReserve = commissionReserve(intermediate)
        val leg2Amount = bestLeg1.amountOut - commissionReserve
        if (leg2Amount <= BigDecimal.ZERO) {
            // User gets less than commission reserve on leg 1, so leg 2 is not profitable
            return@coroutineScope null
        }

        val leg2Quotes = fetchQuotes(leg2Providers, intermediate, tokenOut, leg2Amount, settings)
        if (leg2Quotes.isEmpty()) return@coroutineScope null

        MultiSwapRoute(
            intermediateCoin = intermediate,
            leg1Quotes = leg1Quotes,
            leg2Quotes = leg2Quotes,
            commissionReserve = commissionReserve,
            selectedLeg1Quote = bestLeg1,
            selectedLeg2Quote = leg2Quotes.first(),
        )
    }

    private suspend fun fetchQuotes(
        supported: List<IMultiSwapProvider>,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        settings: Map<String, Any?>,
    ): List<SwapProviderQuote> = coroutineScope {
        supported.map { provider ->
            async {
                try {
                    withTimeoutOrNull(TIMEOUT_MS) {
                        val quote = provider.fetchQuote(tokenIn, tokenOut, amountIn, settings)
                        SwapProviderQuote(provider = provider, swapQuote = quote)
                    }
                } catch (e: Throwable) {
                    Timber.d(e, "fetchQuoteError: ${provider.id}")
                    null
                }
            }
        }.awaitAll()
            .filterNotNull()
            .sortedByBestAmountOut()
    }

    private fun commissionReserve(intermediate: Token): BigDecimal =
        if (intermediate.blockchainType == BlockchainType.Ton) TON_COMMISSION_RESERVE
        else DEFAULT_COMMISSION_RESERVE
}

data class MultiSwapRoute(
    val intermediateCoin: Token,
    val leg1Quotes: List<SwapProviderQuote>,
    val leg2Quotes: List<SwapProviderQuote>,
    val commissionReserve: BigDecimal,
    val selectedLeg1Quote: SwapProviderQuote,
    val selectedLeg2Quote: SwapProviderQuote,
)
