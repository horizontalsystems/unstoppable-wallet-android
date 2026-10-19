package io.horizontalsystems.walletkit.modules.multiswap.providers

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A ZEC sell names its source and refund as one decision per provider: either the transparent
 * refund alone, or the shielded-only unified address as both. The shielded-only address is a
 * fresh one stored in the wallet on every request, so it must not be asked for when it is not
 * going to be named.
 */
class ZcashSellAddressesTest {

    private val transparent = "t1RBzFrCTWSZe3FJbDewtveUMRX2og4kKph"
    private val shieldedOnly = "u1shieldedonly"

    @Test
    fun `a ZEC sell names no source address when the provider takes no unified refund`() = runBlocking {
        val providers = listOf(
            UProvider.QuickEx,
            UProvider.StealthEx,
            UProvider.Exolix,
            UProvider.ThorChain,
            UProvider.Swapuz,
            UProvider.Lizex,
            UProvider.XSwap,
            UProvider.NearConfidential,
        )

        for (provider in providers) {
            val addresses = ZcashSellAddresses.resolve(
                provider = provider,
                transparentAddress = { transparent },
                shieldedOnlyAddress = { throw AssertionError("$provider asked for a shielded-only address") },
            )

            assertNull("$provider named a source", addresses.source)
            assertEquals("$provider refund", transparent, addresses.refund)
        }
    }

    @Test
    fun `a provider taking a unified refund gets the shielded-only address as refund and source`() = runBlocking {
        for (provider in listOf(UProvider.Near, UProvider.Maya)) {
            val addresses = ZcashSellAddresses.resolve(
                provider = provider,
                transparentAddress = { throw AssertionError("$provider asked for the transparent address") },
                shieldedOnlyAddress = { shieldedOnly },
            )

            assertEquals("$provider refund", shieldedOnly, addresses.refund)
            assertEquals("$provider source", shieldedOnly, addresses.source)
        }
    }
}
