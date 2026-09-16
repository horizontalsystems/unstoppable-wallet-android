package io.horizontalsystems.walletkit.chain.zcash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ZnsTest {

    // ── Name normalization ──────────────────────────────────────────────────

    @Test
    fun normalize_acceptsBothSuffixesInAnyCase() {
        assertEquals("alice", ZnsName.normalize("alice.zcash"))
        assertEquals("alice", ZnsName.normalize("alice.zec"))
        assertEquals("alice", ZnsName.normalize("Alice.ZCASH"))
        assertEquals("alice", ZnsName.normalize("aLice.Zec"))
        assertEquals("alice", ZnsName.normalize("  alice.zcash "))
        assertEquals("bob42", ZnsName.normalize("bob42.zec"))
        assertEquals("a", ZnsName.normalize("a.zcash"))
        assertEquals("a".repeat(62), ZnsName.normalize("a".repeat(62) + ".zcash"))
    }

    @Test
    fun normalize_rejectsBareNamesAndAddresses() {
        // Suffix is required: bare input is ambiguous with a partially typed address.
        assertNull(ZnsName.normalize("alice"))
        assertNull(ZnsName.normalize("childish"))
        assertNull(ZnsName.normalize("u1jlu75mjtmhdekpq0nx5kakkgdnss8ta6xqzwu8p278axz0zt3pmep8hs4u0ws0nhlz"))
        assertNull(ZnsName.normalize("t1RBzFrCTWSZe3FJbDewtveUMRX2og4kKph"))
        assertNull(ZnsName.normalize("zs1abc"))
    }

    @Test
    fun normalize_rejectsInvalidNames() {
        assertNull(ZnsName.normalize(""))
        assertNull(ZnsName.normalize(".zcash"))
        assertNull(ZnsName.normalize("my-name.zcash"))
        assertNull(ZnsName.normalize("my_name.zec"))
        assertNull(ZnsName.normalize("has space.zcash"))
        assertNull(ZnsName.normalize("café.zcash"))
        assertNull(ZnsName.normalize("alice.zcash.zcash"))
        assertNull(ZnsName.normalize("alice.eth"))
        assertNull(ZnsName.normalize("a".repeat(63) + ".zcash"))
    }

    // ── Response parsing ────────────────────────────────────────────────────

    @Test
    fun parseResolveResponse_found() {
        val registration = ZnsResolver.parseResolveResponse(CHILDISH_BODY)!!

        assertEquals("childish", registration.name)
        assertEquals(CHILDISH_ADDRESS, registration.address)
        assertEquals(0L, registration.nonce)
        assertEquals("BUY", registration.lastAction)
        assertEquals(CHILDISH_SIGNATURE, registration.signature)
        assertNull(registration.pubkey)
    }

    @Test
    fun parseResolveResponse_notRegistered() {
        assertNull(ZnsResolver.parseResolveResponse("""{"jsonrpc":"2.0","id":1,"result":null}"""))
    }

    @Test
    fun parseResolveResponse_rpcError() {
        val body = """{"jsonrpc":"2.0","id":1,"error":{"code":-32602,"message":"Invalid params"}}"""
        val error = assertThrows(ZnsResolver.RpcError::class.java) {
            ZnsResolver.parseResolveResponse(body)
        }
        assertTrue(error.message!!.contains("-32602"))
    }

    @Test
    fun parseResolveResponse_arrayResultIsRejected() {
        // The indexer answers an address query with an array; a name query never should.
        assertThrows(ZnsResolver.RpcError::class.java) {
            ZnsResolver.parseResolveResponse("""{"jsonrpc":"2.0","id":1,"result":[]}""")
        }
    }

    @Test
    fun parseResolveResponse_missingFields() {
        assertThrows(ZnsResolver.RpcError::class.java) {
            ZnsResolver.parseResolveResponse("""{"jsonrpc":"2.0","id":1,"result":{"name":"alice"}}""")
        }
    }

    @Test
    fun parseResolveResponse_malformed() {
        assertThrows(ZnsResolver.RpcError::class.java) { ZnsResolver.parseResolveResponse("not json") }
        assertThrows(ZnsResolver.RpcError::class.java) { ZnsResolver.parseResolveResponse("") }
        assertThrows(ZnsResolver.RpcError::class.java) { ZnsResolver.parseResolveResponse("[]") }
    }

    // ── Signature verification (live mainnet vectors, 2026-09-15) ──────────

    @Test
    fun preImage_followsLastAction() {
        val base = registration("alice", "u1example", nonce = 2, lastAction = "CLAIM")
        assertEquals("CLAIM:alice:u1example", ZnsResolver.preImage(base))
        assertEquals("BUY:alice:u1example", ZnsResolver.preImage(base.copy(lastAction = "BUY")))
        assertEquals("UPDATE:alice:u1example:2", ZnsResolver.preImage(base.copy(lastAction = "UPDATE")))
        assertEquals("DELIST:alice:2", ZnsResolver.preImage(base.copy(lastAction = "DELIST")))
        assertNull(ZnsResolver.preImage(base.copy(lastAction = "LIST")))
        assertNull(ZnsResolver.preImage(base.copy(lastAction = "RELEASE")))
    }

    @Test
    fun verify_buyRegistration() {
        val registration = ZnsResolver.parseResolveResponse(CHILDISH_BODY)!!
        assertTrue(ZnsResolver.verify(registration, ZnsResolver.ADMIN_PUBKEY))
    }

    @Test
    fun verify_claimRegistration() {
        val registration = registration(
            name = "gaurang",
            address = "u1h743e3e4kstgsddvw6ngu8kpntj34keprem8jm3x5tjh47d4g6kg78x4chy444l5mj2xetpnacml6xv4alsm6vwlsvqlrmkjfqza04lxjzm7ctnr6mzh2kmeqqel235mc660uy3a7cjx85vv5l43gl365xhlywe2zeptkppdkk8kqlhpdlaa09d54jgphqugxledqp9syfu9g7qx8ta",
            nonce = 0,
            lastAction = "CLAIM",
            signature = "VL7SIbtmGGCCKPBNV4XnYMAvV6I6qwvXvKV+KY5VIY65JlSVb1+95mrjuUeiIEyWcu2TPGmUj6m+bvNOkI6zDA==",
        )
        assertTrue(ZnsResolver.verify(registration, ZnsResolver.ADMIN_PUBKEY))
    }

    @Test
    fun verify_updateRegistrationSignedWithEarlierNonce() {
        // zechariah: CLAIM (nonce 0) -> UPDATE signed with nonce 1 -> LIST bumped the registry
        // nonce to 2 without touching last_action or signature. The reported nonce (2) does not
        // verify; the search must find the signed nonce (1).
        val registration = registration(
            name = "zechariah",
            address = "u14lms6q2sg984hhng068lqesatnak86mlg297x0qkqsvkqgk2m7dyj5x2grlku3adg2tq6trl5ue3vce7c3jnu5808kw9lsr890kfetrg524q4pgcn4zcnp84na85453pz67cjfy4sx7y6z6zc9t7666utasw5mehe9r2dwwcrgzp94ah",
            nonce = 2,
            lastAction = "UPDATE",
            signature = "0yNPmGqCQGIMYXOXLmpXRSrOo1B1FWDgpvU1jHrqzvFYz9ZLitNeKB93ANVwpmkU/KD/R6OHK0x9MWIpBDB1BQ==",
        )
        assertTrue(ZnsResolver.verify(registration, ZnsResolver.ADMIN_PUBKEY))
        assertTrue(ZnsResolver.verify(registration.copy(nonce = 1), ZnsResolver.ADMIN_PUBKEY))
        // The search only walks downward, so a reported nonce below the signed one fails.
        assertFalse(ZnsResolver.verify(registration.copy(nonce = 0), ZnsResolver.ADMIN_PUBKEY))
        // CLAIM and BUY do not use the nonce, so no search happens for them.
        assertFalse(ZnsResolver.verify(registration.copy(lastAction = "CLAIM"), ZnsResolver.ADMIN_PUBKEY))
    }

    @Test
    fun verify_rejectsTamperedAddress() {
        val registration = ZnsResolver.parseResolveResponse(CHILDISH_BODY)!!
        val swapped = registration.copy(address = registration.address.dropLast(1) + "h")
        assertFalse(ZnsResolver.verify(swapped, ZnsResolver.ADMIN_PUBKEY))
    }

    @Test
    fun verify_rejectsTamperedSignatureAndWrongKey() {
        val registration = ZnsResolver.parseResolveResponse(CHILDISH_BODY)!!
        val flipped = registration.copy(signature = "A" + registration.signature!!.drop(1))
        assertFalse(ZnsResolver.verify(flipped, ZnsResolver.ADMIN_PUBKEY))

        val otherKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        assertFalse(ZnsResolver.verify(registration, otherKey))
    }

    @Test
    fun verify_rejectsMissingSignatureAndUnknownAction() {
        val registration = ZnsResolver.parseResolveResponse(CHILDISH_BODY)!!
        assertFalse(ZnsResolver.verify(registration.copy(signature = null), ZnsResolver.ADMIN_PUBKEY))
        assertFalse(ZnsResolver.verify(registration.copy(lastAction = "RELEASE"), ZnsResolver.ADMIN_PUBKEY))
        assertFalse(ZnsResolver.verify(registration.copy(signature = "not base64!"), ZnsResolver.ADMIN_PUBKEY))
    }

    @Test
    fun verify_sovereignRegistrationUsesAttachedKey() {
        // An admin-signed row with a foreign pubkey attached must fail: the attached key wins.
        val registration = ZnsResolver.parseResolveResponse(CHILDISH_BODY)!!
        val otherKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        assertFalse(ZnsResolver.verify(registration.copy(pubkey = otherKey), ZnsResolver.ADMIN_PUBKEY))
    }

    // ── Handler ─────────────────────────────────────────────────────────────

    @Test
    fun handler_skipsResolverForNonNames() {
        val handler = AddressHandlerZns(ZnsResolver(url = "http://127.0.0.1:1"))
        assertFalse(handler.isSupported("alice"))
        assertFalse(handler.isSupported("u1jlu75mjtmhdekpq0nx5kakkgdnss8ta6xqzwu8p278axz0zt3pmep8hs4u0ws0nhlz"))
    }

    @Test
    fun handler_unreachableResolverIsNotSupported() {
        val handler = AddressHandlerZns(ZnsResolver(url = "http://127.0.0.1:1"))
        assertFalse(handler.isSupported("alice.zcash"))
    }

    private fun registration(
        name: String,
        address: String,
        nonce: Long,
        lastAction: String,
        signature: String? = "AQID",
        pubkey: String? = null,
    ) = ZnsResolver.Registration(name, address, nonce, lastAction, signature, pubkey)

    companion object {
        private const val CHILDISH_ADDRESS =
            "u1jlu75mjtmhdekpq0nx5kakkgdnss8ta6xqzwu8p278axz0zt3pmep8hs4u0ws0nhlz40925lq770cqw8thnzyp2upm6vwxsc6rrfr3zuvrnjtkrayf9lm09fv3v2erhnu7jme34w2c5x47eafzkks2mwfegphd6aa0dsp0qtxy382nrg"
        private const val CHILDISH_SIGNATURE =
            "9ChHKUP7I+j2KKB4WARAayqWY4ZA6pX7cDwKuuGJOVBypAPxXOlUITrC7+B0gekJfN8Zv2Na1Aa+Og/W3qb0AQ=="

        // Verbatim mainnet response for resolve {"query":"childish"}
        private val CHILDISH_BODY = """
            {"jsonrpc":"2.0","id":1,"result":{"address":"$CHILDISH_ADDRESS","height":3476168,"last_action":"BUY","listing":null,"name":"childish","nonce":0,"signature":"$CHILDISH_SIGNATURE","txid":"8eecefa29c32c9adbc2a0d72d59c7163dd6c744f43c7178caa77d2788dce6e8f"}}
        """.trimIndent()
    }
}
