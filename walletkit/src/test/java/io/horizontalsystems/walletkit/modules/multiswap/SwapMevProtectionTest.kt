package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI.Response.Execution
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwapMevProtectionTest {

    private fun token(blockchainType: BlockchainType, type: TokenType = TokenType.Native) = Token(
        coin = Coin("uid", "Name", "CODE"),
        blockchain = Blockchain(blockchainType, "Name", null),
        type = type,
        decimals = 18,
    )

    private val eth = token(BlockchainType.Ethereum)
    private val usdt = token(BlockchainType.Ethereum, TokenType.Eip20("0xdAC17F958D2ee523a2206206994597C13D831ec7"))
    private val bnb = token(BlockchainType.BinanceSmartChain)
    private val btc = token(BlockchainType.Bitcoin)
    private val sol = token(BlockchainType.Solana)

    private fun execution(method: String) = Execution(
        method = method,
        chain = null,
        transactions = null,
        approval = null,
        depositAddress = null,
        amount = null,
        asset = null,
        attachment = null,
        unsignedTx = null,
        protocol = null,
        inboundAddress = null,
        memo = null,
        delivery = null,
    )

    @Test
    fun `a same-chain evm pair is protectable`() {
        assertTrue(SwapMevProtection.allowedForPair(eth, usdt))
    }

    @Test
    fun `a cross-chain or non-evm pair is not`() {
        assertFalse(SwapMevProtection.allowedForPair(eth, bnb))
        assertFalse(SwapMevProtection.allowedForPair(eth, btc))
        assertFalse(SwapMevProtection.allowedForPair(sol, sol))
    }

    @Test
    fun `only a signed transaction route on such a pair is protectable`() {
        assertTrue(SwapMevProtection.allowedForRoute(execution("signed_transaction"), eth, usdt))
        assertFalse(SwapMevProtection.allowedForRoute(execution("transfer"), eth, usdt))
        assertFalse(SwapMevProtection.allowedForRoute(execution("thorchain_deposit"), eth, usdt))
        assertFalse(SwapMevProtection.allowedForRoute(null, eth, usdt))
        assertFalse(SwapMevProtection.allowedForRoute(execution("signed_transaction"), eth, bnb))
    }
}
