package io.horizontalsystems.walletkit.modules.zcashnetwork.addendpoint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZcashEndpointUrlTest {

    @Test
    fun httpsUrl_isKept() {
        assertEquals("https://zec.rocks:443", ZcashEndpointUrl.parse("https://zec.rocks:443"))
    }

    @Test
    fun surroundingSpaces_areDropped() {
        assertEquals("https://zec.rocks:443", ZcashEndpointUrl.parse("  https://zec.rocks:443 \n"))
    }

    @Test
    fun hostAndPort_getHttps() {
        assertEquals("https://zcash.example.com:443", ZcashEndpointUrl.parse("zcash.example.com:443"))
    }

    @Test
    fun bareHost_getsHttps() {
        assertEquals("https://zcash.example.com", ZcashEndpointUrl.parse("zcash.example.com"))
    }

    @Test
    fun ipAddressAndPort_getHttps() {
        assertEquals("https://192.168.1.20:9067", ZcashEndpointUrl.parse("192.168.1.20:9067"))
    }

    @Test
    fun ipv6AddressAndPort_getHttps() {
        assertEquals("https://[fd00::20]:9067", ZcashEndpointUrl.parse("[fd00::20]:9067"))
    }

    @Test
    fun uppercaseScheme_isAccepted() {
        assertEquals("HTTPS://zec.rocks", ZcashEndpointUrl.parse("HTTPS://zec.rocks"))
    }

    @Test
    fun otherSchemes_areRejected() {
        assertNull(ZcashEndpointUrl.parse("http://zec.rocks:443"))
        assertNull(ZcashEndpointUrl.parse("grpc://zec.rocks:443"))
    }

    @Test
    fun inputWithoutHost_isRejected() {
        assertNull(ZcashEndpointUrl.parse(""))
        assertNull(ZcashEndpointUrl.parse("   "))
        assertNull(ZcashEndpointUrl.parse("https://"))
        assertNull(ZcashEndpointUrl.parse("not a url"))
    }

    @Test
    fun userInfo_isRejected() {
        assertNull(ZcashEndpointUrl.parse("https://user:secret@zec.rocks:443"))
        assertNull(ZcashEndpointUrl.parse("user@zec.rocks"))
        assertNull(ZcashEndpointUrl.parse("mailto:user@example.com"))
    }

    @Test
    fun portOutsideTheValidRange_isRejected() {
        assertNull(ZcashEndpointUrl.parse("zcash.example.com:65536"))
        assertNull(ZcashEndpointUrl.parse("zcash.example.com:0"))
        assertEquals("https://zcash.example.com:65535", ZcashEndpointUrl.parse("zcash.example.com:65535"))
        assertEquals("https://zcash.example.com:1", ZcashEndpointUrl.parse("zcash.example.com:1"))
    }

    @Test
    fun pathQueryAndFragment_areRejected() {
        assertNull(ZcashEndpointUrl.parse("node.example:443/zcash"))
        assertNull(ZcashEndpointUrl.parse("https://node.example/zcash"))
        assertNull(ZcashEndpointUrl.parse("https://node.example:443?x=1"))
        assertNull(ZcashEndpointUrl.parse("https://node.example:443#top"))
    }

    @Test
    fun trailingSlash_isAccepted() {
        assertEquals("https://node.example:443/", ZcashEndpointUrl.parse("node.example:443/"))
    }
}
