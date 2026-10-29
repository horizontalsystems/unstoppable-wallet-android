package io.horizontalsystems.walletkit.modules.multiswap.pathfinder

import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.bonk
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.eth
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.sol
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.stubTokenSource
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.usdcCoin
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.usdcEth
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.usdtCoin
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.usdtEth
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.usdtSol
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPathFixtures.usdtTron
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwapPathCandidateProviderTest {

    @Test
    fun `native tier returns tokenOut chain first`() {
        val provider = NativeSwapPathCandidateProvider(stubTokenSource())

        assertEquals(listOf(sol, eth), provider.candidates(eth, bonk))
    }

    @Test
    fun `native tier collapses when chains match`() {
        val provider = NativeSwapPathCandidateProvider(stubTokenSource())

        assertEquals(listOf(eth), provider.candidates(eth, usdtEth))
    }

    @Test
    fun `native tier drops unknown chain`() {
        val source = stubTokenSource().apply { tokensByQuery.remove(sol.tokenQuery) }
        val provider = NativeSwapPathCandidateProvider(source)

        assertEquals(listOf(eth), provider.candidates(eth, bonk))
    }

    @Test
    fun `coin list tier keeps configured uid order and sorts chains`() {
        val source = stubTokenSource().apply {
            // returned in the opposite order on purpose: fullCoins(coinUids) makes no order promise
            coins = listOf(
                FullCoin(usdcCoin, listOf(usdcEth)),
                FullCoin(usdtCoin, listOf(usdtTron, usdtEth, usdtSol)),
            )
        }
        val provider = CoinListSwapPathCandidateProvider(source, listOf("tether", "usd-coin"))

        assertEquals(listOf(usdtSol, usdtEth, usdtTron, usdcEth), provider.candidates(eth, bonk))
    }

    @Test
    fun `coin list tier ignores unknown uid`() {
        val provider = CoinListSwapPathCandidateProvider(stubTokenSource(), listOf("nope", "usd-coin"))

        assertEquals(listOf(usdcEth), provider.candidates(eth, bonk))
    }

    @Test
    fun `coin list tier degrades to empty when lookup fails`() {
        val source = stubTokenSource().apply { fullCoinsThrows = true }
        val provider = CoinListSwapPathCandidateProvider(source, listOf("tether"))

        assertTrue(provider.candidates(eth, bonk).isEmpty())
    }
}
