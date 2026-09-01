package io.horizontalsystems.walletkit.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class MarketWidgetStateTest {

    private val cachedItem = MarketWidgetItem(
        uid = "bitcoin",
        title = "BTC",
        subtitle = "$1.2T",
        label = "1",
        value = "$60,000",
        diff = BigDecimal("1.5"),
        blockchainTypeUid = null,
        imageRemoteUrl = "https://example.com/btc.png",
    )

    @Test
    fun `offline with cached rows keeps rows and hides error`() {
        val state = MarketWidgetState(items = listOf(cachedItem), loading = true, updateTimestampMillis = 123L)

        val result = state.afterRefreshFailure("No internet")

        assertEquals(listOf(cachedItem), result.items)
        assertNull(result.error)
        assertFalse(result.loading)
        assertEquals(123L, result.updateTimestampMillis)
    }

    @Test
    fun `offline without cached rows shows the error`() {
        val state = MarketWidgetState(items = emptyList(), loading = true)

        val result = state.afterRefreshFailure("No internet")

        assertEquals("No internet", result.error)
        assertFalse(result.loading)
    }

    @Test
    fun `a stale error is cleared once rows exist`() {
        val state = MarketWidgetState(items = listOf(cachedItem), error = "old error")

        assertNull(state.afterRefreshFailure("No internet").error)
    }
}
