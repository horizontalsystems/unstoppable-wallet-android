package io.horizontalsystems.walletkit.modules.multiswap.history

import io.horizontalsystems.walletkit.entities.SwapRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SwapTrackRequestBuilderTest {

    @Test
    fun `a recorded swap is tracked by its uuid alone, whichever id form the record carries`() {
        for (providerId in listOf("NEAR", "u_NEAR", "QUICKEX")) {
            val call = SwapTrackRequestBuilder.build(record(providerId, providerSwapId = "order-1"))

            assertEquals(SwapTrackRequestBuilder.Endpoint.Recorded, call.endpoint)
            assertEquals("order-1", call.request.uuid)
        }
    }

    @Test
    fun `a recorded swap without a uuid is refused before any request is built`() {
        assertThrows(IllegalArgumentException::class.java) {
            SwapTrackRequestBuilder.build(record("NEAR", providerSwapId = null))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SwapTrackRequestBuilder.build(record("NEAR", providerSwapId = ""))
        }
    }

    private fun record(providerId: String, providerSwapId: String?) = SwapRecord(
        accountId = "a", timestamp = 0, providerId = providerId, providerName = providerId,
        tokenInUid = "in", tokenInCoinCode = "IN", tokenInCoinUid = "in", tokenInBadge = null, tokenInBlockchainTypeUid = "bitcoin",
        tokenOutUid = "out", tokenOutCoinCode = "OUT", tokenOutCoinUid = "out", tokenOutBadge = null, tokenOutBlockchainTypeUid = "ethereum",
        amountIn = "1", amountOut = null, amountOutMin = null, recipientAddress = null, sourceAddress = null,
        transactionHash = null, providerSwapId = providerSwapId, fromAsset = null, toAsset = null, depositAddress = null, status = "Depositing",
    )
}
