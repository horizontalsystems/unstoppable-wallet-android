package io.horizontalsystems.walletkit.modules.walletconnect.request

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class Eip712ParserTest {

    private val permit = """
        {
          "types": {
            "EIP712Domain": [
              {"name": "name", "type": "string"},
              {"name": "chainId", "type": "uint256"},
              {"name": "verifyingContract", "type": "address"}
            ],
            "Permit": [
              {"name": "owner", "type": "address"},
              {"name": "spender", "type": "address"}
            ]
          },
          "primaryType": "Permit",
          "domain": {
            "name": "USD Coin",
            "version": "2",
            "chainId": 1,
            "verifyingContract": "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"
          },
          "message": {
            "owner": "0x1111111111111111111111111111111111111111",
            "spender": "0x2222222222222222222222222222222222222222"
          }
        }
    """.trimIndent()

    @Test
    fun `reads domain and primary type`() {
        val data = Eip712Parser.parse(permit)

        assertNotNull(data)
        assertEquals("USD Coin", data?.domainName)
        assertEquals(1L, data?.chainId)
        assertEquals("0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48", data?.verifyingContract)
        assertEquals("Permit", data?.primaryType)
    }

    @Test
    fun `accepts chainId sent as a decimal string`() {
        val json = """{"primaryType":"Permit","domain":{"chainId":"137"}}"""

        assertEquals(137L, Eip712Parser.parse(json)?.chainId)
    }

    @Test
    fun `accepts chainId sent as a hex string`() {
        val json = """{"primaryType":"Permit","domain":{"chainId":"0x89"}}"""

        assertEquals(137L, Eip712Parser.parse(json)?.chainId)
    }

    @Test
    fun `reports missing domain fields as null instead of failing`() {
        val json = """{"primaryType":"Mail","domain":{}}"""
        val data = Eip712Parser.parse(json)

        assertEquals("Mail", data?.primaryType)
        assertNull(data?.chainId)
        assertNull(data?.domainName)
        assertNull(data?.verifyingContract)
    }

    @Test
    fun `returns null for payloads it cannot read`() {
        assertNull(Eip712Parser.parse(""))
        assertNull(Eip712Parser.parse("not json"))
        assertNull(Eip712Parser.parse("[1,2,3]"))
        assertNull(Eip712Parser.parse("{}"))
        // nothing recognisable, so nothing to show
        assertNull(Eip712Parser.parse("""{"message":{"a":1}}"""))
    }

    @Test
    fun `ignores wrongly typed fields rather than throwing`() {
        val json = """{"primaryType":{"a":1},"domain":{"chainId":{"b":2},"name":["x"]}}"""

        assertNull(Eip712Parser.parse(json))
    }

    @Test
    fun `ignores a domain that is not an object`() {
        val json = """{"primaryType":"Permit","domain":"nope"}"""
        val data = Eip712Parser.parse(json)

        assertEquals("Permit", data?.primaryType)
        assertNull(data?.chainId)
    }
}
