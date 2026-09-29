package io.horizontalsystems.walletkit.core.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.net.URISyntaxException

class OnionNodeUrlTest {

    private fun matches(url: String) = OnionNodeUrl.isHttpOnion(URI(url))

    // --- accepted HTTP onion hosts ---

    @Test
    fun `accepts http onion hosts`() {
        assertTrue(matches("http://example.onion"))
        assertTrue(matches("http://abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopqrstuvwxyz2345.onion"))
    }

    @Test
    fun `accepts uppercase onion hostname spelling`() {
        assertTrue(matches("http://EXAMPLE.ONION"))
        assertTrue(matches("http://Example.Onion"))
    }

    @Test
    fun `accepts onion subdomains`() {
        assertTrue(matches("http://rpc.example.onion"))
        assertTrue(matches("http://a.b.example.onion"))
    }

    @Test
    fun `accepts explicit ports`() {
        assertTrue(matches("http://example.onion:18081"))
        assertTrue(matches("http://rpc.example.onion:8545"))
    }

    @Test
    fun `accepts rpc paths and query strings`() {
        assertTrue(matches("http://example.onion/json_rpc"))
        assertTrue(matches("http://example.onion/rpc?batch=1"))
        assertTrue(matches("http://rpc.example.onion:18081/json_rpc?foo=bar"))
    }

    // --- rejected hosts and placements ---

    @Test
    fun `rejects ordinary domains and ips`() {
        assertFalse(matches("http://example.com"))
        assertFalse(matches("HTTP://example.onion"))
        assertFalse(matches("http://node.xmr.rocks"))
        assertFalse(matches("http://127.0.0.1"))
        assertFalse(matches("http://192.168.1.10:8545"))
        assertFalse(matches("http://[::1]"))
    }

    @Test
    fun `rejects bare onion`() {
        assertFalse(matches("http://onion"))
        assertFalse(matches("http://onion/"))
        assertFalse(matches("http://.onion"))
    }

    @Test
    fun `rejects hostless and opaque uris`() {
        assertFalse(matches("http:opaque"))
        assertFalse(matches("http:///path"))
        assertFalse(matches("mailto:user@example.onion"))
    }

    @Test
    fun `rejects onion as a label of an ordinary domain`() {
        assertFalse(matches("http://onion.example.com"))
        assertFalse(matches("http://example.onion.com"))
    }

    @Test
    fun `rejects notonion lookalikes`() {
        assertFalse(matches("http://notonion"))
        assertFalse(matches("http://example.notonion"))
        assertFalse(matches("http://onionextra.com"))
    }

    @Test
    fun `rejects onion occurring only in a path or query`() {
        assertFalse(matches("http://example.com/foo.onion"))
        assertFalse(matches("http://example.com/?q=foo.onion"))
        assertFalse(matches("http://example.com/#example.onion"))
    }

    @Test
    fun `rejects onion userinfo before an ordinary host`() {
        assertFalse(matches("http://example.onion@example.com"))
        assertFalse(matches("http://user:example.onion@example.com"))
    }

    // --- schemes ---

    @Test
    fun `rejects unsupported schemes including ftp and ws`() {
        assertFalse(matches("ftp://example.onion"))
        assertFalse(matches("ws://example.onion"))
        assertFalse(matches("wss://example.onion"))
        assertFalse(matches("https://example.onion"))
    }

    @Test
    fun `does not treat https or wss public hosts as the http onion exception`() {
        assertFalse(matches("https://example.com"))
        assertFalse(matches("wss://example.com"))
        assertFalse(matches("https://example.com:8545/rpc"))
    }

    @Test
    fun `malformed uri input throws before the predicate is consulted`() {
        assertThrows(URISyntaxException::class.java) {
            URI("http://[malformed")
        }
        assertThrows(URISyntaxException::class.java) {
            URI("http:// example.onion")
        }
    }
}
