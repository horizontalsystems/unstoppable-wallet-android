package io.horizontalsystems.walletkit.modules.multiswap.pathfinder

import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider

/**
 * Finds a one-hop route for a pair that can't be swapped directly.
 *
 * Pure matching over injected candidate tiers: the only I/O is whatever the candidate providers
 * do, and the only [App] touch is the [default] factory.
 */
class SwapPathFinder(private val candidateProviders: List<ISwapPathCandidateProvider>) {

    fun find(
        tokenIn: Token,
        tokenOut: Token,
        providers: List<IMultiSwapProvider>,
    ): SwapPath? {
        // Same gate as SwapQuoteService.runQuotation: a leg we would refuse to ask for must not
        // count, or we would advertise a route no provider can be asked about.
        fun usable(provider: IMultiSwapProvider, from: Token, to: Token): Boolean =
            provider.supports(from, to)

        val endpointIds = setOf(tokenIn.tokenQuery.id, tokenOut.tokenQuery.id)
        val triedIds = mutableSetOf<String>()

        // Declared tier order IS the ranking: first hit wins, no scoring.
        for (candidateProvider in candidateProviders) {
            for (candidate in candidateProvider.candidates(tokenIn, tokenOut)) {
                val id = candidate.tokenQuery.id

                if (id in endpointIds || !triedIds.add(id)) continue

                // No point asking about the second leg with no way in.
                if (providers.none { usable(it, tokenIn, candidate) }) continue
                if (providers.none { usable(it, candidate, tokenOut) }) continue

                return SwapPath(tokenIn, candidate, tokenOut)
            }
        }

        return null
    }

    companion object {
        fun default(marketKit: MarketKitWrapper = App.marketKit): SwapPathFinder {
            val tokenSource = MarketKitSwapPathTokenSource(marketKit)
            return SwapPathFinder(
                listOf(
                    NativeSwapPathCandidateProvider(tokenSource),
                    CoinListSwapPathCandidateProvider(tokenSource),
                )
            )
        }
    }
}
