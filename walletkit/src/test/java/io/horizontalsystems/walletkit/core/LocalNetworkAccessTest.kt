package io.horizontalsystems.walletkit.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class LocalNetworkAccessTest {

    // An IP literal is parsed without a DNS lookup.
    private fun isLocal(ip: String) = LocalNetworkAccess.isLocal(InetAddress.getByName(ip))

    @Test
    fun privateIpv4Ranges_areLocal() {
        assertTrue(isLocal("10.0.0.1"))
        assertTrue(isLocal("10.255.255.254"))
        assertTrue(isLocal("172.16.0.1"))
        assertTrue(isLocal("172.31.255.254"))
        assertTrue(isLocal("192.168.0.1"))
        assertTrue(isLocal("192.168.255.254"))
    }

    @Test
    fun linkLocalAddresses_areLocal() {
        assertTrue(isLocal("169.254.10.10"))
        assertTrue(isLocal("fe80::1"))
    }

    @Test
    fun uniqueLocalIpv6_isLocal() {
        assertTrue(isLocal("fd00::1"))
        assertTrue(isLocal("fc00::1"))
        assertTrue(isLocal("fdff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"))
    }

    @Test
    fun publicAddresses_areNotLocal() {
        assertFalse(isLocal("8.8.8.8"))
        assertFalse(isLocal("172.15.255.254"))
        assertFalse(isLocal("172.32.0.1"))
        assertFalse(isLocal("192.169.0.1"))
        assertFalse(isLocal("2001:4860:4860::8888"))
        assertFalse(isLocal("fe00::1"))
    }

    @Test
    fun loopback_isNotLocal() {
        assertFalse(isLocal("127.0.0.1"))
        assertFalse(isLocal("::1"))
    }

    @Test
    fun urlWithLocalAddress_isLocal() = runBlocking {
        assertTrue(LocalNetworkAccess.isLocalUrl("https://192.168.1.20:9067"))
        assertTrue(LocalNetworkAccess.isLocalUrl("https://10.0.0.5"))
        assertTrue(LocalNetworkAccess.isLocalUrl("https://[fd00::20]:9067"))
    }

    @Test
    fun urlWithPublicAddress_isNotLocal() = runBlocking {
        assertFalse(LocalNetworkAccess.isLocalUrl("https://8.8.8.8:443"))
        assertFalse(LocalNetworkAccess.isLocalUrl("https://[2001:4860:4860::8888]:443"))
        assertFalse(LocalNetworkAccess.isLocalUrl("https://127.0.0.1:9067"))
    }

    @Test
    fun urlWithoutHost_isNotLocal() = runBlocking {
        assertFalse(LocalNetworkAccess.isLocalUrl(""))
        assertFalse(LocalNetworkAccess.isLocalUrl("not a url"))
        assertFalse(LocalNetworkAccess.isLocalUrl("192.168.1.20:9067"))
    }

    @Test
    fun dotLocalName_isLocalWithoutBeingResolved() = runBlocking {
        assertTrue(LocalNetworkAccess.isLocalHost("lightwallet.local"))
        assertTrue(LocalNetworkAccess.isLocalHost("Lightwallet.LOCAL."))
        assertTrue(LocalNetworkAccess.isLocalUrl("https://lightwallet.local:9067"))
    }

    @Test
    fun classification_tellsLocalFromPublic() = runBlocking {
        assertEquals(true, LocalNetworkAccess.classifyUrl("https://192.168.1.20:9067"))
        assertEquals(false, LocalNetworkAccess.classifyUrl("https://8.8.8.8:443"))
        assertEquals(false, LocalNetworkAccess.classifyUrl("not a url"))
        assertEquals(true, LocalNetworkAccess.classifyHost("lightwallet.local"))
    }
}
