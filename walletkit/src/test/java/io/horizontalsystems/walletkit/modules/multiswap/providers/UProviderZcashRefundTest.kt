package io.horizontalsystems.walletkit.modules.multiswap.providers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Pins which providers take a unified ZEC refund. The flag is app data (the server publishes
 * nothing about accepted ZEC address types), so the table is the policy: a provider added with
 * the flag set by mistake would have its ZEC orders refused at commit, and one missing it would
 * refund to the transparent pool.
 */
class UProviderZcashRefundTest {

    @Test
    fun `only Maya and NEAR take a unified ZEC refund`() {
        val flagged = UProvider.entries.filter { it.acceptsUnifiedZcashRefund }

        assertEquals(listOf(UProvider.Near, UProvider.Maya), flagged)
    }

    @Test
    fun `the confidential NEAR rail refunds to the transparent address`() {
        assertFalse(UProvider.NearConfidential.acceptsUnifiedZcashRefund)
    }
}
