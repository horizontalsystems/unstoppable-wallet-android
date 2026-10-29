package io.horizontalsystems.walletkit.modules.multiswap.pathfinder

import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.bonk
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.candidates
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.edge
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.eth
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.provider
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.sol
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.suspensions
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.trx
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.usdtSol
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapSuspensionIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Tier order (native first, then the coin list) IS the ranking: the finder returns on the first hit.
class SwapPathFinderTest {

    private val noSuspensions = SwapSuspensionIndex()

    @Test
    fun `no candidates returns null`() {
        val finder = SwapPathFinder(emptyList())

        assertNull(finder.find(eth, bonk, listOf(provider(edges = emptyList())), noSuspensions))
    }

    @Test
    fun `finds intermediate via native tier`() {
        val providers = listOf(provider("a", listOf(edge(eth, sol), edge(sol, bonk))))
        val finder = SwapPathFinder(listOf(candidates(sol)))

        val path = finder.find(eth, bonk, providers, noSuspensions)

        assertEquals(SwapPath(eth, sol, bonk), path)
    }

    @Test
    fun `native tier wins over coin list tier`() {
        val providers = listOf(
            provider("a", listOf(edge(eth, sol), edge(sol, bonk), edge(eth, usdtSol), edge(usdtSol, bonk)))
        )
        val finder = SwapPathFinder(listOf(candidates(sol), candidates(usdtSol)))

        assertEquals(sol, finder.find(eth, bonk, providers, noSuspensions)?.intermediateToken)
    }

    @Test
    fun `tokenOut native tried before tokenIn native`() {
        // Both natives link the pair; tokenOut's chain is the binding constraint, so SOL wins.
        val providers = listOf(
            provider("a", listOf(edge(eth, sol), edge(sol, bonk), edge(eth, trx), edge(trx, bonk)))
        )
        val finder = SwapPathFinder(listOf(candidates(sol, trx)))

        assertEquals(sol, finder.find(eth, bonk, providers, noSuspensions)?.intermediateToken)
    }

    @Test
    fun `legs may use different providers`() {
        val providers = listOf(
            provider("a", listOf(edge(eth, sol))),
            provider("b", listOf(edge(sol, bonk))),
        )
        val finder = SwapPathFinder(listOf(candidates(sol)))

        assertNotNull(finder.find(eth, bonk, providers, noSuspensions))
    }

    @Test
    fun `rejects candidate with no second leg`() {
        val providers = listOf(provider("a", listOf(edge(eth, sol))))
        val finder = SwapPathFinder(listOf(candidates(sol)))

        assertNull(finder.find(eth, bonk, providers, noSuspensions))
    }

    @Test
    fun `continues to next candidate after rejected one`() {
        // SOL has an inbound leg but no outbound one; the coin-list tier must still be reached.
        val providers = listOf(
            provider("a", listOf(edge(eth, sol), edge(eth, usdtSol), edge(usdtSol, bonk)))
        )
        val finder = SwapPathFinder(listOf(candidates(sol), candidates(usdtSol)))

        assertEquals(usdtSol, finder.find(eth, bonk, providers, noSuspensions)?.intermediateToken)
    }

    @Test
    fun `skips second leg when first leg empty`() {
        val log = mutableListOf<String>()
        val providers = listOf(provider("a", listOf(edge(sol, bonk)), log))
        val finder = SwapPathFinder(listOf(candidates(sol)))

        finder.find(eth, bonk, providers, noSuspensions)

        assertEquals(listOf(edge(eth, sol)), log)
    }

    @Test
    fun `respects suspensions`() {
        // The one provider that could serve leg two is suspended for exactly that directed pair.
        val providers = listOf(provider("a", listOf(edge(eth, sol), edge(sol, bonk))))
        val finder = SwapPathFinder(listOf(candidates(sol)))

        assertNull(finder.find(eth, bonk, providers, suspensions("a", sell = sol, buy = bonk)))
    }

    @Test
    fun `suspended provider does not count toward leg`() {
        val providers = listOf(
            provider("a", listOf(edge(eth, sol), edge(sol, bonk))),
            provider("b", listOf(edge(sol, bonk))),
        )
        val finder = SwapPathFinder(listOf(candidates(sol)))

        assertNotNull(finder.find(eth, bonk, providers, suspensions("a", sell = sol, buy = bonk)))
    }

    @Test
    fun `skips endpoints as candidates`() {
        val log = mutableListOf<String>()
        val providers = listOf(provider("a", listOf(edge(eth, bonk)), log))
        val finder = SwapPathFinder(listOf(candidates(eth, bonk)))

        assertNull(finder.find(eth, bonk, providers, noSuspensions))
        assertTrue(log.isEmpty())
    }

    @Test
    fun `deduplicates across tiers`() {
        val log = mutableListOf<String>()
        val providers = listOf(provider("a", emptyList(), log))
        val finder = SwapPathFinder(listOf(candidates(sol), candidates(sol)))

        finder.find(eth, bonk, providers, noSuspensions)

        assertEquals(listOf(edge(eth, sol)), log)
    }
}
