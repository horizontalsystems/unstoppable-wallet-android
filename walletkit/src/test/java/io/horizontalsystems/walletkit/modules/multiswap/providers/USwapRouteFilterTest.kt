package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class USwapRouteFilterTest {

    private fun token(type: BlockchainType) =
        Token(Coin(type.uid, type.uid, type.uid.uppercase()), Blockchain(type, type.uid, null), TokenType.Native, 8)

    @Test
    fun `a stellar broker route is never executable`() {
        assertFalse(USwapRouteFilter.isExecutable("stellar_broker", token(BlockchainType.Stellar)))
    }

    @Test
    fun `a vault deposit is executable only where the memo reaches the vault`() {
        for (type in listOf(BlockchainType.Bitcoin, BlockchainType.Litecoin, BlockchainType.Ethereum, BlockchainType.Zcash, BlockchainType.Thorchain, BlockchainType.Ton)) {
            assertTrue(type.uid, USwapRouteFilter.isExecutable("thorchain_deposit", token(type)))
        }
        for (type in listOf(BlockchainType.Xrp, BlockchainType.Solana, BlockchainType.Tron, BlockchainType.Monero, BlockchainType.Zano)) {
            assertFalse(type.uid, USwapRouteFilter.isExecutable("thorchain_deposit", token(type)))
        }
    }

    @Test
    fun `transfer, signed and unknown execution types are kept`() {
        val xrp = token(BlockchainType.Xrp)

        assertTrue(USwapRouteFilter.isExecutable("transfer", xrp))
        assertTrue(USwapRouteFilter.isExecutable("signed_transaction", xrp))
        assertTrue(USwapRouteFilter.isExecutable("something_new", xrp))
        assertTrue(USwapRouteFilter.isExecutable(null, xrp))
    }

    @Test
    fun `a route naming no provider is dropped`() {
        val named = route(listOf("NEAR"))
        val unnamed = route(null)
        val empty = route(listOf(""))

        val kept = USwapRouteFilter.filter(listOf(named, unnamed, empty), token(BlockchainType.Bitcoin)) { null }

        assertEquals(listOf(named), kept)
    }

    private fun route(providers: List<String>?) = UnstoppableAPI.Response.Route(
        sellAsset = null, buyAsset = null, expectedBuyAmount = null, minBuyAmount = null, estimatedTime = null,
        approvalSpender = null, execution = null, uuid = null, providers = providers,
    )
}
