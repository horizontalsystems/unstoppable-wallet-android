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
    private val permit2Contract = "0x000000000022D473030F116dDEE9F6B43aC78BA3"

    private val eip2612Types = """
        "Permit": [
          {"name": "owner", "type": "address"},
          {"name": "spender", "type": "address"},
          {"name": "value", "type": "uint256"},
          {"name": "nonce", "type": "uint256"},
          {"name": "deadline", "type": "uint256"}
        ]
    """.trimIndent()

    private fun permitJson(value: String, types: String = eip2612Types) = """
        {
          "types": { $types },
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

    private fun permitSingleJson(verifyingContract: String = permit2Contract) = """
        {
          "types": {
            "PermitSingle": [
              {"name": "details", "type": "PermitDetails"},
              {"name": "spender", "type": "address"},
              {"name": "sigDeadline", "type": "uint256"}
            ],
            "PermitDetails": [
              {"name": "token", "type": "address"},
              {"name": "amount", "type": "uint160"},
              {"name": "expiration", "type": "uint48"},
              {"name": "nonce", "type": "uint48"}
            ]
          },
          "primaryType": "PermitSingle",
          "domain": { "name": "Permit2", "chainId": 1, "verifyingContract": "$verifyingContract" },
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

    @Test
    fun `reads an eip-2612 permit, taking the token from the verifying contract`() {
        val permit = Eip712Parser.parse(permitJson("1000000"))?.permit

        assertEquals("0xToken", permit?.token)
        assertEquals("0xSpender", permit?.spender)
        assertEquals(BigInteger("1000000"), permit?.amount)
        // EIP-2612 deadline gates the signature only; the allowance it writes has no expiry
        assertEquals(BigInteger("1799999999"), permit?.signatureDeadline)
        assertNull(permit?.allowanceExpires)
        assertFalse(permit!!.unlimited)
    }

    @Test
    fun `flags an unlimited eip-2612 permit at the uint256 ceiling`() {
        assertTrue(Eip712Parser.parse(permitJson(maxUint256))?.permit?.unlimited == true)
    }

    @Test
    fun `does not treat a uint160 sized eip-2612 amount as unlimited`() {
        // uint160 max is only "no limit" for Permit2; for a uint256 permit it is a finite allowance
        val permit = Eip712Parser.parse(permitJson(maxUint160))?.permit

        assertEquals(BigInteger(maxUint160), permit?.amount)
        assertFalse(permit!!.unlimited)
    }

    @Test
    fun `reads a permit2 PermitSingle, taking the token and expiry from details`() {
        val permit = Eip712Parser.parse(permitSingleJson())?.permit

        assertEquals("0xTokenAddr", permit?.token)
        assertEquals("0xUniversalRouter", permit?.spender)
        // the allowance lifetime and the signature window are separate values
        assertEquals(BigInteger("1800000000"), permit?.allowanceExpires)
        assertEquals(BigInteger("1700000000"), permit?.signatureDeadline)
        // uint160 max is the Permit2 way of saying no limit
        assertTrue(permit!!.unlimited)
    }

    // --- a summary is only offered for a shape that proves itself ---

    @Test
    fun `refuses a struct that borrows the Permit name`() {
        // same primaryType and message keys, entirely different declared type
        val spoofed = """
            "Permit": [
              {"name": "owner", "type": "address"},
              {"name": "spender", "type": "address"},
              {"name": "value", "type": "string"}
            ]
        """.trimIndent()

        val data = Eip712Parser.parse(permitJson("1000000", types = spoofed))

        assertEquals("Permit", data?.primaryType)
        assertNull(data?.permit)
    }

    @Test
    fun `refuses a Permit whose fields are reordered`() {
        val reordered = """
            "Permit": [
              {"name": "spender", "type": "address"},
              {"name": "owner", "type": "address"},
              {"name": "value", "type": "uint256"},
              {"name": "nonce", "type": "uint256"},
              {"name": "deadline", "type": "uint256"}
            ]
        """.trimIndent()

        assertNull(Eip712Parser.parse(permitJson("1000000", types = reordered))?.permit)
    }

    @Test
    fun `refuses a PermitSingle addressed to something other than Permit2`() {
        // the structs match, but a different contract declaring them is not Permit2
        assertNull(Eip712Parser.parse(permitSingleJson("0xDecoy"))?.permit)
    }

    @Test
    fun `refuses a permit with no type declarations at all`() {
        val json = """
            {
              "primaryType": "Permit",
              "domain": { "verifyingContract": "0xToken" },
              "message": { "spender": "0xSpender", "value": "1" }
            }
        """.trimIndent()

        assertNull(Eip712Parser.parse(json)?.permit)
    }

    @Test
    fun `leaves non-permit typed data without a permit`() {
        val json = """
            {
              "types": { "Mail": [{"name": "contents", "type": "string"}] },
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
              "types": { "PermitBatch": [{"name": "spender", "type": "address"}] },
              "primaryType": "PermitBatch",
              "domain": { "verifyingContract": "$permit2Contract" },
              "message": { "details": [], "spender": "0xSpender" }
            }
        """.trimIndent()

        assertNull(Eip712Parser.parse(json)?.permit)
    }

    @Test
    fun `survives a declared permit whose message values are missing or wrongly typed`() {
        val json = """
            {
              "types": { $eip2612Types },
              "primaryType": "Permit",
              "domain": { "verifyingContract": "0xToken" },
              "message": { "spender": 42, "value": "abc" }
            }
        """.trimIndent()

        val permit = Eip712Parser.parse(json)?.permit

        assertEquals("0xToken", permit?.token)
        assertNull(permit?.spender)
        assertNull(permit?.amount)
        assertNull(permit?.signatureDeadline)
        assertFalse(permit!!.unlimited)
    }

    // --- timestamps ---

    @Test
    fun `reports an out of range deadline as no timestamp rather than wrapping it`() {
        // dApps send a huge value to mean "no deadline"; it must not become a nonsensical date
        val permit = Eip712Parser.parse(
            permitJson("1000000").replace("\"deadline\": \"1799999999\"", "\"deadline\": \"$maxUint256\"")
        )?.permit

        assertEquals(BigInteger(maxUint256), permit?.signatureDeadline)
        assertNull(permit?.signatureDeadline?.asTimestampOrNull())
    }

    @Test
    fun `converts an in range deadline to a timestamp`() {
        val permit = Eip712Parser.parse(permitJson("1000000"))?.permit

        assertEquals(1799999999L * 1000, permit?.signatureDeadline?.asTimestampOrNull()?.time)
    }
}
