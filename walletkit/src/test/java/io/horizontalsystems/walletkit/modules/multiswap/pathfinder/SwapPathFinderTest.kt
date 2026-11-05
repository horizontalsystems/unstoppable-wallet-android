package io.horizontalsystems.walletkit.modules.multiswap.pathfinder

import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.bonk
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.candidates
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.edge
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.eth
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.provider
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.sol
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.trx
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.usdtSol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Tier order (native first, then the coin list) IS the ranking: the finder returns on the first hit.
class SwapPathFinderTest {

    @Test
    fun `no candidates returns null`() {
        val finder = SwapPathFinder(emptyList())

        assertNull(finder.find(eth, bonk, listOf(provider(edges = emptyList()))))
    }

    @Test
    fun `finds intermediate via native tier`() {
        val providers = listOf(provider("a", listOf(edge(eth, sol), edge(sol, bonk))))
        val finder = SwapPathFinder(listOf(candidates(sol)))

        val path = finder.find(eth, bonk, providers)

        assertEquals(SwapPath(eth, sol, bonk), path)
    }

    @Test
    fun `native tier wins over coin list tier`() {
        val providers = listOf(
            provider("a", listOf(edge(eth, sol), edge(sol, bonk), edge(eth, usdtSol), edge(usdtSol, bonk)))
        )
        val finder = SwapPathFinder(listOf(candidates(sol), candidates(usdtSol)))

        assertEquals(sol, finder.find(eth, bonk, providers)?.intermediateToken)
    }

    @Test
    fun `tokenOut native tried before tokenIn native`() {
        // Both natives link the pair; tokenOut's chain is the binding constraint, so SOL wins.
        val providers = listOf(
            provider("a", listOf(edge(eth, sol), edge(sol, bonk), edge(eth, trx), edge(trx, bonk)))
        )
        val finder = SwapPathFinder(listOf(candidates(sol, trx)))

        assertEquals(sol, finder.find(eth, bonk, providers)?.intermediateToken)
    }

    @Test
    fun `legs may use different providers`() {
        val providers = listOf(
            provider("a", listOf(edge(eth, sol))),
            provider("b", listOf(edge(sol, bonk))),
        )
        val finder = SwapPathFinder(listOf(candidates(sol)))

        assertNotNull(finder.find(eth, bonk, providers))
    }

    @Test
    fun `rejects candidate with no second leg`() {
        val providers = listOf(provider("a", listOf(edge(eth, sol))))
        val finder = SwapPathFinder(listOf(candidates(sol)))

        assertNull(finder.find(eth, bonk, providers))
    }

    @Test
    fun `continues to next candidate after rejected one`() {
        // SOL has an inbound leg but no outbound one; the coin-list tier must still be reached.
        val providers = listOf(
            provider("a", listOf(edge(eth, sol), edge(eth, usdtSol), edge(usdtSol, bonk)))
        )
        val finder = SwapPathFinder(listOf(candidates(sol), candidates(usdtSol)))

        assertEquals(usdtSol, finder.find(eth, bonk, providers)?.intermediateToken)
    }

    @Test
    fun `skips second leg when first leg empty`() {
        val log = mutableListOf<String>()
        val providers = listOf(provider("a", listOf(edge(sol, bonk)), log))
        val finder = SwapPathFinder(listOf(candidates(sol)))

        finder.find(eth, bonk, providers)

        assertEquals(listOf(edge(eth, sol)), log)
    }

    @Test
    fun `skips endpoints as candidates`() {
        val log = mutableListOf<String>()
        val providers = listOf(provider("a", listOf(edge(eth, bonk)), log))
        val finder = SwapPathFinder(listOf(candidates(eth, bonk)))

        assertNull(finder.find(eth, bonk, providers))
        assertTrue(log.isEmpty())
    }

    @Test
    fun `deduplicates across tiers`() {
        val log = mutableListOf<String>()
        val providers = listOf(provider("a", emptyList(), log))
        val finder = SwapPathFinder(listOf(candidates(sol), candidates(sol)))

        finder.find(eth, bonk, providers)

        assertEquals(listOf(edge(eth, sol)), log)
    }
}
