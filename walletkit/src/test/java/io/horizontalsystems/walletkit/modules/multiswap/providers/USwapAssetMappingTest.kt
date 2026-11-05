package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class USwapAssetMappingTest {

    private fun catalogToken(identifier: String, chainId: String, address: String? = null, ticker: String? = null) =
        UnstoppableAPI.Response.CatalogToken(identifier = identifier, chainId = chainId, address = address, ticker = ticker)

    private fun token(type: BlockchainType, tokenType: TokenType) =
        Token(Coin("c", "c", "C"), Blockchain(type, type.uid, null), tokenType, 8)

    @Test
    fun `a catalog row resolves to the wallet token it names`() {
        assertEquals(
            listOf(TokenQuery(BlockchainType.Ethereum, TokenType.Eip20("0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"))),
            USwapAssetMapping.tokenQueries(catalogToken("ETH.USDC-0X…", "1", "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48")),
        )
        assertEquals(listOf(TokenQuery(BlockchainType.Ethereum, TokenType.Native)), USwapAssetMapping.tokenQueries(catalogToken("ETH.ETH", "1")))
        assertEquals(listOf(TokenQuery(BlockchainType.Solana, TokenType.Spl("mint"))), USwapAssetMapping.tokenQueries(catalogToken("SOL.X-mint", "solana", "mint")))
        assertEquals(
            listOf(TokenQuery(BlockchainType.Stellar, TokenType.Asset("USDC", "GA5Z"))),
            USwapAssetMapping.tokenQueries(catalogToken("XLM.USDC-GA5Z", "stellar", "GA5Z", "USDC")),
        )
        assertEquals(listOf(TokenQuery(BlockchainType.Near, TokenType.Nep141("usdt.tether-token.near"))), USwapAssetMapping.tokenQueries(catalogToken("NEAR.USDT", "near", "usdt.tether-token.near")))
    }

    @Test
    fun `a UTXO coin resolves to every derivation the wallet may hold it under, LTC without taproot`() {
        val btc = USwapAssetMapping.tokenQueries(catalogToken("BTC.BTC", "bitcoin"))
        assertTrue(btc.size > 1)
        assertTrue(btc.all { it.blockchainType == BlockchainType.Bitcoin })

        val ltc = USwapAssetMapping.tokenQueries(catalogToken("LTC.LTC", "litecoin"))
        assertTrue(ltc.none { (it.tokenType as? TokenType.Derived)?.derivation == TokenType.Derivation.Bip86 })
    }

    @Test
    fun `rows on chains the app does not know, and issued XRP assets, resolve to nothing`() {
        assertTrue(USwapAssetMapping.tokenQueries(catalogToken("DOGE.DOGE", "dogecoin")).isEmpty())
        assertTrue(USwapAssetMapping.tokenQueries(catalogToken("XRP.RLUSD-r…", "ripple", "rISSUER")).isEmpty())
        assertEquals(listOf(TokenQuery(BlockchainType.Xrp, TokenType.Native)), USwapAssetMapping.tokenQueries(catalogToken("XRP.XRP", "ripple")))
    }

    @Test
    fun `a token the catalog lacks is named by its chain-prefixed address`() {
        assertEquals("ETH.0xabc", USwapAssetMapping.derivedIdentifier(token(BlockchainType.Ethereum, TokenType.Eip20("0xabc"))))
        assertEquals("BASE.0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", USwapAssetMapping.derivedIdentifier(token(BlockchainType.Base, TokenType.Native)))
        assertEquals("SOL.MintCaseKept", USwapAssetMapping.derivedIdentifier(token(BlockchainType.Solana, TokenType.Spl("MintCaseKept"))))
        assertEquals("SOL.${USwapAssetMapping.WSOL_MINT}", USwapAssetMapping.derivedIdentifier(token(BlockchainType.Solana, TokenType.Native)))
        assertEquals("TRON.TRX", USwapAssetMapping.derivedIdentifier(token(BlockchainType.Tron, TokenType.Native)))
        assertEquals("TRON.TR7N", USwapAssetMapping.derivedIdentifier(token(BlockchainType.Tron, TokenType.Eip20("TR7N"))))
        assertEquals("XLM.yXLM-GISSUER", USwapAssetMapping.derivedIdentifier(token(BlockchainType.Stellar, TokenType.Asset("yXLM", "GISSUER"))))
    }

    @Test
    fun `tokens with no addressable form cannot be named`() {
        // the wSOL token is read as native SOL by the server, so it is not offered
        assertNull(USwapAssetMapping.derivedIdentifier(token(BlockchainType.Solana, TokenType.Spl(USwapAssetMapping.WSOL_MINT))))
        // a Soroban contract token is not a classic asset
        assertNull(USwapAssetMapping.derivedIdentifier(token(BlockchainType.Stellar, TokenType.Asset("X", "CCONTRACT"))))
        // a UTXO coin has no address form; it is quotable only through the catalog
        assertNull(USwapAssetMapping.derivedIdentifier(token(BlockchainType.Bitcoin, TokenType.Native)))
        // a chain without a known code
        assertNull(USwapAssetMapping.derivedIdentifier(token(BlockchainType.Gnosis, TokenType.Eip20("0xabc"))))
    }

    @Test
    fun `the chain hint is the numeric id of an EVM chain only`() {
        assertEquals("1", USwapAssetMapping.chainId(BlockchainType.Ethereum))
        assertEquals("56", USwapAssetMapping.chainId(BlockchainType.BinanceSmartChain))
        assertNull(USwapAssetMapping.chainId(BlockchainType.Solana))
        assertNull(USwapAssetMapping.chainId(BlockchainType.Bitcoin))
    }

    @Test
    fun `an EVM contract address keys the catalog in one case, other tokens by their id`() {
        val checksummed = TokenQuery(BlockchainType.Ethereum, TokenType.Eip20("0xdAC17F958D2ee523a2206206994597C13D831ec7"))
        val lowercase = TokenQuery(BlockchainType.Ethereum, TokenType.Eip20("0xdac17f958d2ee523a2206206994597c13d831ec7"))
        assertEquals(lowercase.id, USwapAssetMapping.catalogKey(checksummed))
        assertEquals(lowercase.id, USwapAssetMapping.catalogKey(lowercase))

        val tron = TokenQuery(BlockchainType.Tron, TokenType.Eip20("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"))
        assertEquals(tron.id, USwapAssetMapping.catalogKey(tron))
        val native = TokenQuery(BlockchainType.Ethereum, TokenType.Native)
        assertEquals(native.id, USwapAssetMapping.catalogKey(native))
    }
}
