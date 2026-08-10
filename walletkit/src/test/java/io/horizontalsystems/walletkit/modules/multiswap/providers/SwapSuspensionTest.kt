package io.horizontalsystems.walletkit.modules.multiswap.providers

import com.google.gson.Gson
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SwapSuspensionTest {

    private fun token(blockchainType: BlockchainType, type: TokenType) = Token(
        coin = Coin("uid", "Name", "CODE"),
        blockchain = Blockchain(blockchainType, "Name", null),
        type = type,
        decimals = 8,
    )

    private val btc = token(BlockchainType.Bitcoin, TokenType.Native)
    private val usdc = token(BlockchainType.Ethereum, TokenType.Eip20("0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"))
    private val eth = token(BlockchainType.Ethereum, TokenType.Native)
    private val sol = token(BlockchainType.Solana, TokenType.Native)

    private fun index(vararg rules: SwapSuspension) = SwapSuspensionIndex(
        rulesByProvider = mapOf("NEAR" to rules.toList()),
    )

    // --- canonical asset ids ---

    @Test
    fun `native tokens use the chain gas ticker, not the chain code`() {
        assertEquals("BTC.BTC", CanonicalAssetId.of(btc))
        assertEquals("BASE.ETH", CanonicalAssetId.of(token(BlockchainType.Base, TokenType.Native)))
        assertEquals("BSC.BNB", CanonicalAssetId.of(token(BlockchainType.BinanceSmartChain, TokenType.Native)))
        assertEquals("GNO.XDAI", CanonicalAssetId.of(token(BlockchainType.Gnosis, TokenType.Native)))
    }

    @Test
    fun `derivation and address type collapse into the native asset`() {
        val bip84 = token(BlockchainType.Bitcoin, TokenType.Derived(TokenType.Derivation.Bip84))
        val type145 = token(BlockchainType.BitcoinCash, TokenType.AddressTyped(TokenType.AddressType.Type145))

        assertEquals("BTC.BTC", CanonicalAssetId.of(bip84))
        assertEquals("BCH.BCH", CanonicalAssetId.of(type145))
    }

    @Test
    fun `contract addresses are upper-cased`() {
        assertEquals("ETH.0XA0B86991C6218B36C1D19D4A2E9EB0CE3606EB48", CanonicalAssetId.of(usdc))
    }

    @Test
    fun `wrapped sol is native sol`() {
        val wsol = token(BlockchainType.Solana, TokenType.Spl("So11111111111111111111111111111111111111112"))

        assertEquals("SOL.SOL", CanonicalAssetId.of(wsol))
    }

    @Test
    fun `stellar keeps the case-sensitive asset code and upper-cases the issuer`() {
        val yxlm = token(BlockchainType.Stellar, TokenType.Asset("yXLM", "gabc"))

        assertEquals("XLM.yXLM-GABC", CanonicalAssetId.of(yxlm))
    }

    @Test
    fun `thorchain secured assets stay unprefixed and read as thorchain`() {
        val secured = token(BlockchainType.Thorchain, TokenType.ThorchainAsset("eth-eth"))

        assertEquals("ETH-ETH", CanonicalAssetId.of(secured))
        assertEquals("THOR", CanonicalAssetId.chainOf("ETH-ETH"))
        assertEquals("ETH", CanonicalAssetId.chainOf("ETH.0XA0B8"))
    }

    @Test
    fun `an unmapped token yields no id`() {
        assertNull(CanonicalAssetId.of(token(BlockchainType.Zano, TokenType.ZanoAsset("ref"))))
        assertNull(CanonicalAssetId.of(token(BlockchainType.Unsupported("nope"), TokenType.Native)))
    }

    // --- matching ---

    @Test
    fun `asset rule without a side covers both sides`() {
        val rule = SwapSuspension(kind = SwapSuspension.Kind.Asset, asset = "BTC.BTC")

        assertTrue(index(rule).isSuspended("u_NEAR", btc, usdc))
        assertTrue(index(rule).isSuspended("u_NEAR", usdc, btc))
        assertFalse(index(rule).isSuspended("u_NEAR", usdc, eth))
    }

    @Test
    fun `a sided asset rule leaves the other side quotable`() {
        val rule = SwapSuspension(
            kind = SwapSuspension.Kind.Asset,
            side = SwapSuspension.Side.Sell,
            asset = "BTC.BTC",
        )

        assertTrue(index(rule).isSuspended("u_NEAR", btc, usdc))
        assertFalse(index(rule).isSuspended("u_NEAR", usdc, btc))
    }

    @Test
    fun `chain rule covers every asset on the chain`() {
        val rule = SwapSuspension(kind = SwapSuspension.Kind.Chain, chain = "ETH")

        assertTrue(index(rule).isSuspended("u_NEAR", usdc, btc))
        assertTrue(index(rule).isSuspended("u_NEAR", eth, btc))
        assertFalse(index(rule).isSuspended("u_NEAR", btc, sol))
    }

    @Test
    fun `pair rule is directed`() {
        val rule = SwapSuspension(
            kind = SwapSuspension.Kind.Pair,
            sellAsset = "BTC.BTC",
            buyAsset = "ETH.ETH",
        )

        assertTrue(index(rule).isSuspended("u_NEAR", btc, eth))
        assertFalse(index(rule).isSuspended("u_NEAR", eth, btc))
    }

    @Test
    fun `an expired rule stops applying without a resync`() {
        val expired = SwapSuspension(
            kind = SwapSuspension.Kind.Asset,
            asset = "BTC.BTC",
            expiresAt = Instant.now().minusSeconds(60).toString(),
        )
        val live = expired.copy(expiresAt = Instant.now().plusSeconds(60).toString())

        assertFalse(index(expired).isSuspended("u_NEAR", btc, usdc))
        assertTrue(index(live).isSuspended("u_NEAR", btc, usdc))
    }

    @Test
    fun `a malformed expiry keeps the rule in force`() {
        val rule = SwapSuspension(kind = SwapSuspension.Kind.Asset, asset = "BTC.BTC", expiresAt = "soon")

        assertTrue(index(rule).isSuspended("u_NEAR", btc, usdc))
    }

    @Test
    fun `a rule this build cannot evaluate is inert`() {
        val unknownKind = SwapSuspension(kind = null, asset = "BTC.BTC")
        val unmappedToken = SwapSuspension(kind = SwapSuspension.Kind.Chain, chain = "ZANO")
        val zano = token(BlockchainType.Zano, TokenType.ZanoAsset("ref"))

        assertFalse(index(unknownKind).isSuspended("u_NEAR", btc, usdc))
        assertFalse(index(unmappedToken).isSuspended("u_NEAR", zano, btc))
    }

    @Test
    fun `rules only bind the provider they were published for`() {
        val rule = SwapSuspension(kind = SwapSuspension.Kind.Asset, asset = "BTC.BTC")

        assertFalse(index(rule).isSuspended("u_EXOLIX", btc, usdc))
    }

    @Test
    fun `a suspended provider serves no pair`() {
        val suspended = SwapSuspensionIndex(suspendedProviders = setOf("ONEINCH"))

        assertTrue(suspended.isSuspended("oneinch", usdc, eth))
        assertFalse(suspended.isSuspended("uniswap_v3", usdc, eth))
    }

    // --- wire and cache formats ---

    @Test
    fun `rules parse from the server payload`() {
        val json = """
            [
              {"provider":"NEAR","suspensions":[{"kind":"pair","side":"sell","sellAsset":"BTC.BTC","buyAsset":"ETH.ETH"}]},
              {"provider":"EXOLIX","suspended":true},
              {"provider":"ONEINCH"}
            ]
        """.trimIndent()
        val responses = Gson().fromJson(json, Array<UnstoppableAPI.Response.Provider>::class.java).toList()

        val index = SwapSuspensionIndex.from(responses)

        assertEquals(setOf("EXOLIX"), index.suspendedProviders)
        assertEquals(setOf("NEAR"), index.rulesByProvider.keys)
        assertTrue(index.isSuspended("u_NEAR", btc, eth))
        assertTrue(index.isSuspended("u_EXOLIX", btc, eth))
        assertFalse(index.isSuspended("oneinch", btc, eth))
    }

    @Test
    fun `the index survives a cache round-trip`() {
        val gson = Gson()
        val index = SwapSuspensionIndex(
            suspendedProviders = setOf("EXOLIX"),
            rulesByProvider = mapOf(
                "NEAR" to listOf(
                    SwapSuspension(
                        kind = SwapSuspension.Kind.Chain,
                        side = SwapSuspension.Side.Buy,
                        chain = "ETH",
                        expiresAt = "2030-01-01T00:00:00Z",
                    )
                )
            ),
        )

        val restored = gson.fromJson(gson.toJson(index), SwapSuspensionIndex::class.java)

        assertEquals(index, restored)
        assertTrue(restored.isSuspended("u_NEAR", btc, usdc))
        assertFalse(restored.isSuspended("u_NEAR", usdc, btc))
    }
}
