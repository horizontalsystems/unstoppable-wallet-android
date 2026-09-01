package io.horizontalsystems.walletkit.modules.main

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDeepLinksTest {

    private fun isMarket(link: String) = MarketDeepLinks.isMarketDeepLink(link, "unstoppable")

    @Test
    fun `widget coin and platform links are market links`() {
        assertTrue(isMarket("unstoppable://coin-page?uid=bitcoin"))
        assertTrue(isMarket("unstoppable://top-platforms?uid=ethereum&title=Ethereum"))
        assertTrue(isMarket("unstoppable://coin-page"))
    }

    @Test
    fun `other app scheme links are not market links`() {
        assertFalse(isMarket("unstoppable://wc?uri=wc:abc"))
        assertFalse(isMarket("unstoppable://referral?userId=1"))
        assertFalse(isMarket("unstoppable://coin-page-x?uid=bitcoin"))
    }

    @Test
    fun `foreign schemes are not market links`() {
        assertFalse(isMarket("bitcoin:bc1qxyz"))
        assertFalse(isMarket("wc:abc@2?relay"))
        assertFalse(isMarket("https://unstoppable.money/referral?userId=1"))
        assertFalse(isMarket("myscheme://coin-page?uid=bitcoin"))
    }
}
