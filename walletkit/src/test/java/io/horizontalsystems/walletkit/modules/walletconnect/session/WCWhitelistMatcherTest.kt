package io.horizontalsystems.walletkit.modules.walletconnect.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WCWhitelistMatcherTest {

    private val whiteList = listOf("https://uniswap.org", "https://app.aave.com")

    private fun matches(url: String) = WCWhitelistMatcher.isHostInWhiteList(url, whiteList)

    // --- spoofing attempts must be rejected ---

    @Test
    fun `rejects lookalike domain that ends with a trusted domain`() {
        assertFalse(matches("https://eviluniswap.org"))
        assertFalse(matches("https://not-uniswap.org"))
    }

    @Test
    fun `rejects trusted domain placed in the path of an untrusted host`() {
        assertFalse(matches("https://evil.com/uniswap.org"))
        assertFalse(matches("https://evil.com/#/uniswap.org"))
    }

    @Test
    fun `rejects trusted domain placed in userinfo`() {
        assertFalse(matches("https://uniswap.org@evil.com"))
    }

    @Test
    fun `rejects a shorter domain that a trusted domain ends with`() {
        // "uniswap.org".endsWith("swap.org") must not make swap.org trusted
        assertFalse(matches("https://swap.org"))
    }

    @Test
    fun `rejects parent domain when only a subdomain is whitelisted`() {
        // app.aave.com is whitelisted; the parent is a different origin and fails closed
        assertFalse(matches("https://aave.com"))
    }

    @Test
    fun `does not trust apex when only the www host is whitelisted`() {
        // www is a label like any other, so it must not stand in for the apex
        assertFalse(
            WCWhitelistMatcher.isHostInWhiteList(
                "https://uniswap.org",
                listOf("https://www.uniswap.org")
            )
        )
    }

    @Test
    fun `rejects unrelated and unparseable input`() {
        assertFalse(matches("https://example.com"))
        assertFalse(matches(""))
        assertFalse(matches("   "))
        assertFalse(matches("not a url"))
    }

    // --- legitimate origins must still match ---

    @Test
    fun `matches exact host`() {
        assertTrue(matches("https://uniswap.org"))
        assertTrue(matches("http://uniswap.org"))
    }

    @Test
    fun `matches real subdomain`() {
        assertTrue(matches("https://app.uniswap.org"))
        assertTrue(matches("https://info.app.uniswap.org"))
    }

    @Test
    fun `treats www as a subdomain and ignores trailing slash and case`() {
        assertTrue(matches("https://WWW.Uniswap.ORG/"))
    }

    @Test
    fun `matches when the url carries a path query or fragment`() {
        // the previous string comparison failed these outright
        assertTrue(matches("https://uniswap.org/swap"))
        assertTrue(matches("https://app.uniswap.org/#/swap?chain=mainnet"))
    }

    @Test
    fun `matches bare domain entries without a scheme`() {
        assertTrue(WCWhitelistMatcher.isHostInWhiteList("https://uniswap.org", listOf("uniswap.org")))
    }

    // --- host extraction ---

    @Test
    fun `hostOf normalizes and fails closed`() {
        assertEquals("www.uniswap.org", WCWhitelistMatcher.hostOf("https://WWW.Uniswap.org./swap"))
        assertEquals("evil.com", WCWhitelistMatcher.hostOf("https://uniswap.org@evil.com"))
        assertNull(WCWhitelistMatcher.hostOf(""))
        assertNull(WCWhitelistMatcher.hostOf("http://"))
    }
}
