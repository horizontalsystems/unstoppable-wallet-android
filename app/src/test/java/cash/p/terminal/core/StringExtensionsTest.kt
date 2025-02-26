package cash.p.terminal.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StringExtensionsTest {

    @Test
    fun testBuildAddressesWithMixedInput() {
        val input = "192.168.1.1:8080,example.com:3000\nlocalhost:5000,server.org:21"
        val expected = listOf("192.168.1.1", "example.com", "localhost", "server.org")

        val result = input.splitToAddresses()
        assertEquals(expected, result)
    }

    @Test
    fun testBuildAddressesWithOnlyIPs() {
        val input = "192.168.0.1:8080,10.0.0.1:1234\n127.0.0.1:5000"
        val expected = listOf("192.168.0.1", "10.0.0.1", "127.0.0.1")

        val result = input.splitToAddresses()
        assertEquals(expected, result)
    }

    @Test
    fun testBuildAddressesWithOnlyDomains() {
        val input = "example.com:8080,sub.domain.org:443\napi.service.net:9090"
        val expected = listOf("example.com", "sub.domain.org", "api.service.net")

        val result = input.splitToAddresses()
        assertEquals(expected, result)
    }

    @Test
    fun testBuildAddressesWithNoPorts() {
        val input = "192.168.1.1,example.com\nlocalhost"
        val expected = listOf("192.168.1.1", "example.com", "localhost")

        val result = input.splitToAddresses()
        assertEquals(expected, result)
    }

    @Test
    fun testBuildAddressesWithEmptyInput() {
        val input = ""

        val result = input.splitToAddresses()
        assertTrue(result.isEmpty())
    }
}
