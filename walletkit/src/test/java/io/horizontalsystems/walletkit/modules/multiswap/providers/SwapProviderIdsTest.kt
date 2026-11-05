package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.walletkit.entities.SwapRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwapProviderIdsTest {

    @Test
    fun `a record's provider id resolves to the server id whichever form it carries`() {
        assertEquals("NEAR", SwapProviderIds.serverId("NEAR"))
        assertEquals("NEAR", SwapProviderIds.serverId("u_NEAR"))
        assertEquals("THORCHAIN", SwapProviderIds.serverId("thorchain"))
        assertEquals("MAYACHAIN", SwapProviderIds.serverId("mayachain"))
        assertEquals("PANCAKESWAP", SwapProviderIds.serverId("pancake_v3"))
        assertEquals("ALLBRIDGE", SwapProviderIds.serverId("allbridge"))
    }

    @Test
    fun `only the providers the app once quoted itself are legacy`() {
        assertTrue(SwapProviderIds.isLegacyNative("oneinch"))
        assertFalse(SwapProviderIds.isLegacyNative("u_NEAR"))
        assertFalse(SwapProviderIds.isLegacyNative("NEAR"))
    }

    @Test
    fun `a same-chain signed route is a single transaction, a deposit or a bridge is not`() {
        val types = mapOf("ONEINCH" to "signed_transaction", "LIFI" to "signed_transaction", "NEAR" to "transfer", "THORCHAIN" to "thorchain_deposit")
        val executionType: (String) -> String? = { types[it] }

        assertTrue(SwapProviderIds.isSingleTransactionSwap(record("ONEINCH", "ethereum", "ethereum"), executionType))
        assertTrue(SwapProviderIds.isSingleTransactionSwap(record("u_LIFI", "ethereum", "ethereum"), executionType))
        assertFalse(SwapProviderIds.isSingleTransactionSwap(record("LIFI", "ethereum", "solana"), executionType))
        assertFalse(SwapProviderIds.isSingleTransactionSwap(record("NEAR", "ethereum", "ethereum"), executionType))
        assertFalse(SwapProviderIds.isSingleTransactionSwap(record("THORCHAIN", "bitcoin", "ethereum"), executionType))
        assertFalse(SwapProviderIds.isSingleTransactionSwap(record("NEWVENUE", "ethereum", "ethereum"), executionType))
    }

    @Test
    fun `legacy native records keep their layout without the directory`() {
        val none: (String) -> String? = { null }

        assertTrue(SwapProviderIds.isSingleTransactionSwap(record("oneinch", "ethereum", "ethereum"), none))
        assertTrue(SwapProviderIds.isSingleTransactionSwap(record("uniswap_v3", "ethereum", "ethereum"), none))
        assertFalse(SwapProviderIds.isSingleTransactionSwap(record("thorchain", "bitcoin", "ethereum"), none))
        assertTrue(SwapProviderIds.isSingleTransactionSwap(record("allbridge", "ethereum", "ethereum"), none))
        assertFalse(SwapProviderIds.isSingleTransactionSwap(record("allbridge", "ethereum", "solana"), none))
    }

    private fun record(providerId: String, chainIn: String, chainOut: String) = SwapRecord(
        accountId = "a", timestamp = 0, providerId = providerId, providerName = providerId,
        tokenInUid = "in", tokenInCoinCode = "IN", tokenInCoinUid = "in", tokenInBadge = null, tokenInBlockchainTypeUid = chainIn,
        tokenOutUid = "out", tokenOutCoinCode = "OUT", tokenOutCoinUid = "out", tokenOutBadge = null, tokenOutBlockchainTypeUid = chainOut,
        amountIn = "1", amountOut = null, amountOutMin = null, recipientAddress = null, sourceAddress = null,
        transactionHash = null, providerSwapId = null, fromAsset = null, toAsset = null, depositAddress = null, status = "Depositing",
    )
}
