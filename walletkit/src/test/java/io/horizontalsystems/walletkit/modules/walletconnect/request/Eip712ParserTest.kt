package io.horizontalsystems.walletkit.modules.walletconnect.request

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

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
    fun `ignores boolean and numeric values in string fields`() {
        // Gson stringifies these, so without a string check they would render as "true" or "42"
        val json = """{"primaryType":true,"domain":{"name":42,"verifyingContract":false}}"""

        assertNull(Eip712Parser.parse(json))
    }

    @Test
    fun `keeps readable fields when a sibling field is wrongly typed`() {
        val json = """{"primaryType":"Permit","domain":{"name":42,"chainId":1}}"""
        val data = Eip712Parser.parse(json)

        assertEquals("Permit", data?.primaryType)
        assertEquals(1L, data?.chainId)
        assertNull(data?.domainName)
    }

    @Test
    fun `ignores a domain that is not an object`() {
        val json = """{"primaryType":"Permit","domain":"nope"}"""
        val data = Eip712Parser.parse(json)

        assertEquals("Permit", data?.primaryType)
        assertNull(data?.chainId)
    }

    // --- permit decoding ---

    private val maxUint256 = "115792089237316195423570985008687907853269984665640564039457584007913129639935"
    private val maxUint160 = "1461501637330902918203684832716283019655932542975"

    private fun permitJson(value: String) = """
        {
          "primaryType": "Permit",
          "domain": { "name": "USD Coin", "chainId": 1, "verifyingContract": "0xToken" },
          "message": {
            "owner": "0xOwner",
            "spender": "0xSpender",
            "value": "$value",
            "nonce": "0",
            "deadline": "1799999999"
          }
        }
    """.trimIndent()

    @Test
    fun `reads an eip-2612 permit, taking the token from the verifying contract`() {
        val permit = Eip712Parser.parse(permitJson("1000000"))?.permit

        assertEquals("0xToken", permit?.token)
        assertEquals("0xSpender", permit?.spender)
        assertEquals(BigInteger("1000000"), permit?.amount)
        assertEquals(1799999999L, permit?.deadlineSeconds)
        assertFalse(permit!!.unlimited)
    }

    @Test
    fun `flags an unlimited eip-2612 permit at the uint256 ceiling`() {
        assertTrue(Eip712Parser.parse(permitJson(maxUint256))?.permit?.unlimited == true)
    }

    @Test
    fun `reads a permit2 PermitSingle, taking the token and expiry from details`() {
        val json = """
            {
              "primaryType": "PermitSingle",
              "domain": { "name": "Permit2", "chainId": 1, "verifyingContract": "0xPermit2" },
              "message": {
                "details": {
                  "token": "0xTokenAddr",
                  "amount": "$maxUint160",
                  "expiration": "1800000000",
                  "nonce": "0"
                },
                "spender": "0xUniversalRouter",
                "sigDeadline": "1700000000"
              }
            }
        """.trimIndent()

        val permit = Eip712Parser.parse(json)?.permit

        assertEquals("0xTokenAddr", permit?.token)
        assertEquals("0xUniversalRouter", permit?.spender)
        // the allowance lifetime, not sigDeadline
        assertEquals(1800000000L, permit?.deadlineSeconds)
        // uint160 max is the Permit2 way of saying no limit
        assertTrue(permit!!.unlimited)
    }

    @Test
    fun `leaves non-permit typed data without a permit`() {
        val json = """
            {
              "primaryType": "Mail",
              "domain": { "name": "Ether Mail", "chainId": 1 },
              "message": { "from": "Cow", "to": "Bob", "contents": "Hello, Bob!" }
            }
        """.trimIndent()

        val data = Eip712Parser.parse(json)

        assertEquals("Mail", data?.primaryType)
        assertNull(data?.permit)
    }

    @Test
    fun `does not summarise a PermitBatch`() {
        // several allowances at once cannot be shown in one row, so it stays raw
        val json = """
            {
              "primaryType": "PermitBatch",
              "domain": { "verifyingContract": "0xPermit2" },
              "message": { "details": [], "spender": "0xSpender" }
            }
        """.trimIndent()

        assertNull(Eip712Parser.parse(json)?.permit)
    }

    @Test
    fun `survives a permit with missing or wrongly typed fields`() {
        val json = """
            {
              "primaryType": "Permit",
              "domain": { "verifyingContract": "0xToken" },
              "message": { "spender": 42, "value": "abc" }
            }
        """.trimIndent()

        val permit = Eip712Parser.parse(json)?.permit

        assertEquals("0xToken", permit?.token)
        assertNull(permit?.spender)
        assertNull(permit?.amount)
        assertNull(permit?.deadlineSeconds)
        assertFalse(permit!!.unlimited)
    }
}
