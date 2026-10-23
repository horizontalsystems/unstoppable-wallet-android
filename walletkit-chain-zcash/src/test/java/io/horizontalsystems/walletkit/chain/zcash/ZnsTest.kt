package io.horizontalsystems.walletkit.chain.zcash

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
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
        assertEquals("thebackupkid", ZnsName.normalize(" TheBackupKid.ZEC "))
        assertEquals("bob42", ZnsName.normalize("bob42.zec"))
        assertEquals("a", ZnsName.normalize("a.zcash"))
        assertEquals("a".repeat(63), ZnsName.normalize("a".repeat(63) + ".zcash"))
    }

    @Test
    fun normalize_foldsKelvinSign() {
        assertEquals("kid", ZnsName.normalize("Kid.zec"))
    }

    @Test
    fun normalize_rejectsBareNamesAndAddresses() {
        // Suffix is required: bare input is ambiguous with a partially typed address.
        assertNull(ZnsName.normalize("alice"))
        assertNull(ZnsName.normalize("thebackupkid"))
        assertNull(ZnsName.normalize(THEBACKUPKID_ADDRESS))
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
        assertNull(ZnsName.normalize("alice\n.zcash"))
        assertNull(ZnsName.normalize("alice.eth"))
        assertNull(ZnsName.normalize("a".repeat(64) + ".zcash"))
    }

    // ── Registry check ──────────────────────────────────────────────────────

    @Test
    fun mainnetViewingKey_isThePublishedKey() {
        // Literal copied from the ZNS docs; do not replace it with the constant.
        assertEquals(
            "uview1209ld9y4fr2sel9q87kfjg7xm2sy53e3qf7dvcw3w8jwhhjpldl8z0vk0a6hveg4jk7xcn0rc9mr8yg77djfpc0uxrm5pqzt8q7ul4w0nvs3x5q9vmk9nm2crywtqaealek4mp47ur7ywu7g4g4xcdlwv6gnqzvu5panv5je2ps6rxmx4856unlt5wde8fpz8uhrqchmwhn2j252h3cyx5phad877xezerm6ex8l5kcfjw02ll7m0nspfufamh3ae9ea48395elptspnv6vvc70j2gcjrdze9syt8kpr2kc67flvg8687thdkpha7lpt9r4p9wjty90q2v2yjanqsr2zrecrqs7lfx9cpkmshf7jyh2vvwfqnyzwtpuy7p72hm2mjhperdafvm9u5rwa6s6urcy0uw30f687e3jnmas45qfqtwyz39kf6kxklfrdn7t4hkz56jegmlr8s8hwmt9wa8vz8zlmm76lrmzyj82nzpeqfu572njq",
            ZnsResolver.MAINNET_VIEWING_KEY,
        )
    }

    @Test
    fun verifyRegistry_acceptsLiveStatus() {
        ZnsResolver.verifyRegistry(STATUS_BODY, ZnsResolver.MAINNET_VIEWING_KEY)
    }

    @Test
    fun verifyRegistry_rejectsOtherKeys() {
        val key = ZnsResolver.MAINNET_VIEWING_KEY
        listOf(
            "uviewtest1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq",
            key.dropLast(1),
            key + "q",
            key.uppercase(),
        ).forEach { reported ->
            assertThrows(ZnsResolver.UnknownRegistry::class.java) {
                ZnsResolver.verifyRegistry(statusBody("\"$reported\""), key)
            }
        }
    }

    @Test
    fun verifyRegistry_rejectsMalformedStatus() {
        val key = ZnsResolver.MAINNET_VIEWING_KEY
        listOf(
            """{"jsonrpc":"2.0","id":1,"result":{"synced":true}}""",
            statusBody("42"),
            statusBody("null"),
            """{"jsonrpc":"2.0","id":1,"result":null}""",
            """{"jsonrpc":"2.0","id":1,"result":[]}""",
            """{"jsonrpc":"2.0","id":1}""",
            "not json",
        ).forEach { body ->
            assertThrows(ZnsResolver.RpcError::class.java) { ZnsResolver.verifyRegistry(body, key) }
        }
    }

    @Test
    fun verifyRegistry_rpcError() {
        val body = """{"jsonrpc":"2.0","id":1,"error":{"code":-32601,"message":"Method not found"}}"""
        val error = assertThrows(ZnsResolver.RpcError::class.java) {
            ZnsResolver.verifyRegistry(body, ZnsResolver.MAINNET_VIEWING_KEY)
        }
        assertTrue(error.message!!.contains("-32601"))
    }

    // ── Response parsing (live mainnet vectors, 2026-10-23) ─────────────────

    @Test
    fun parseResolveResponse_liveRegistrations() {
        assertEquals(
            ZnsResolver.Registration("thebackupkid", THEBACKUPKID_ADDRESS),
            ZnsResolver.parseResolveResponse(THEBACKUPKID_BODY),
        )
        assertEquals(
            ZnsResolver.Registration("zechariah", ZECHARIAH_ADDRESS),
            ZnsResolver.parseResolveResponse(ZECHARIAH_BODY),
        )
    }

    @Test
    fun parseResolveResponse_ignoresExpiry() {
        listOf("\"none\"", "\"1\"", "\"4102444800\"", "\"garbage\"", "null").forEach { expiresAt ->
            val body = THEBACKUPKID_BODY.replace("\"expires_at\":\"none\"", "\"expires_at\":$expiresAt")
            assertEquals(THEBACKUPKID_ADDRESS, ZnsResolver.parseResolveResponse(body)!!.address)
        }
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
        assertTrue(error.message!!.contains("Invalid params"))
    }

    @Test
    fun parseResolveResponse_arrayResultIsRejected() {
        // The Resolver answers an address query with an array; a name query never should.
        assertThrows(ZnsResolver.RpcError::class.java) {
            ZnsResolver.parseResolveResponse("""{"jsonrpc":"2.0","id":1,"result":[]}""")
        }
    }

    @Test
    fun parseResolveResponse_missingOrWrongFields() {
        listOf(
            """{"name":"alice"}""",
            """{"address":"u1abc"}""",
            """{"name":"alice","address":42}""",
            """{"name":"alice","address":null}""",
            """{"name":7,"address":"u1abc"}""",
            "\"alice\"",
        ).forEach { result ->
            assertThrows(ZnsResolver.RpcError::class.java) {
                ZnsResolver.parseResolveResponse("""{"jsonrpc":"2.0","id":1,"result":$result}""")
            }
        }
    }

    @Test
    fun parseResolveResponse_malformed() {
        assertThrows(ZnsResolver.RpcError::class.java) { ZnsResolver.parseResolveResponse("not json") }
        assertThrows(ZnsResolver.RpcError::class.java) { ZnsResolver.parseResolveResponse("") }
        assertThrows(ZnsResolver.RpcError::class.java) { ZnsResolver.parseResolveResponse("[]") }
    }

    // ── Validation ──────────────────────────────────────────────────────────

    @Test
    fun validate_returnsAddressForMatchingName() {
        val registration = ZnsResolver.parseResolveResponse(THEBACKUPKID_BODY)!!
        assertEquals(THEBACKUPKID_ADDRESS, ZnsResolver.validate("thebackupkid", registration))
    }

    @Test
    fun validate_rejectsRegistrationForAnotherName() {
        val registration = ZnsResolver.parseResolveResponse(THEBACKUPKID_BODY)!!
        listOf("alice", "thebackupkid.zec", "TheBackupKid").forEach { name ->
            assertThrows(ZnsResolver.RpcError::class.java) { ZnsResolver.validate(name, registration) }
        }

        // Canonically equal but byte-different names do not match.
        val decomposed = ZnsResolver.Registration("café", "u1abc")
        assertThrows(ZnsResolver.RpcError::class.java) { ZnsResolver.validate("café", decomposed) }
    }

    // ── Handler ─────────────────────────────────────────────────────────────

    @Test
    fun handler_skipsResolverForNonNames() {
        val resolver = RecordingResolver(THEBACKUPKID_ADDRESS)
        val handler = AddressHandlerZns(resolver, FakeAddressHandler())
        assertFalse(handler.isSupported("thebackupkid"))
        assertFalse(handler.isSupported(THEBACKUPKID_ADDRESS))
        assertFalse(handler.isSupported("my-name.zec"))
        assertTrue(resolver.queries.isEmpty())
    }

    @Test
    fun handler_resolvesNormalizedName() {
        val resolver = RecordingResolver(THEBACKUPKID_ADDRESS)
        val handler = AddressHandlerZns(resolver, FakeAddressHandler())

        assertTrue(handler.isSupported(" TheBackupKid.ZEC "))
        assertEquals(listOf("thebackupkid"), resolver.queries)

        val address = handler.parseAddress(" TheBackupKid.ZEC ")
        assertEquals(THEBACKUPKID_ADDRESS, address.hex)
        assertEquals("TheBackupKid.ZEC", address.domain)
    }

    @Test
    fun handler_passesResolvedAddressThroughZcashHandler() {
        val addressHandler = FakeAddressHandler()
        AddressHandlerZns(RecordingResolver(ZECHARIAH_ADDRESS), addressHandler).isSupported("zechariah.zcash")
        assertEquals(listOf(ZECHARIAH_ADDRESS), addressHandler.checked)
    }

    @Test
    fun handler_invalidResolvedAddressIsNotSupported() {
        val handler = AddressHandlerZns(RecordingResolver("u1garbage"), FakeAddressHandler())
        assertFalse(handler.isSupported("thebackupkid.zec"))
    }

    @Test
    fun handler_unregisteredNameIsNotSupported() {
        assertFalse(AddressHandlerZns(RecordingResolver(null), FakeAddressHandler()).isSupported("childish.zec"))
    }

    @Test
    fun handler_resolverErrorIsNotSupported() {
        val failing = object : ZnsResolver() {
            override fun resolve(name: String): String? = throw UnknownRegistry()
        }
        assertFalse(AddressHandlerZns(failing, FakeAddressHandler()).isSupported("thebackupkid.zec"))
    }

    @Test
    fun handler_unreachableResolverIsNotSupported() {
        val handler = AddressHandlerZns(ZnsResolver(url = "http://127.0.0.1:1"), FakeAddressHandler())
        assertFalse(handler.isSupported("alice.zcash"))
    }

    // Stands in for AddressHandlerZcash, whose SDK backend needs the native library.
    private class FakeAddressHandler : IAddressHandler {
        override val blockchainType = BlockchainType.Zcash
        val checked = mutableListOf<String>()

        override fun isSupported(value: String): Boolean {
            checked += value
            return value == THEBACKUPKID_ADDRESS || value == ZECHARIAH_ADDRESS
        }

        override fun parseAddress(value: String) = Address(value, blockchainType = blockchainType)
    }

    private class RecordingResolver(private val address: String?) : ZnsResolver(url = "http://127.0.0.1:1") {
        val queries = mutableListOf<String>()

        override fun resolve(name: String): String? {
            queries += name
            return address
        }
    }

    companion object {
        private const val THEBACKUPKID_ADDRESS =
            "u19qfnukkpz06w60e7wmgfqv4l0xeyyaprtz59fulemxk0527xnuyewp8gqck3f2kljq29hw7dz8f595pyt5yns5lz6cvyczn2ky7uwplfdkl4m2kgr5ljuam4m3l6mpvdklcut3640txl6z0xduk0lnsqv2zagatlc4extw8j2zyx8829enfkd72xu2r3kt4qrapfty0cttrzyc52ak9"
        private const val ZECHARIAH_ADDRESS =
            "u14lms6q2sg984hhng068lqesatnak86mlg297x0qkqsvkqgk2m7dyj5x2grlku3adg2tq6trl5ue3vce7c3jnu5808kw9lsr890kfetrg524q4pgcn4zcnp84na85453pz67cjfy4sx7y6z6zc9t7666utasw5mehe9r2dwwcrgzp94ah"

        // Verbatim mainnet responses, 2026-10-23
        private val THEBACKUPKID_BODY = """
            {"jsonrpc":"2.0","id":1,"result":{"name":"thebackupkid","address":"$THEBACKUPKID_ADDRESS","txid":"fedc19a5dd139eb3eaea275163c5671abd13c709e696f2be2a8ff2dd8f73025c","height":3520312,"last_action":"claim","expires_at":"none"}}
        """.trimIndent()
        private val ZECHARIAH_BODY = """
            {"jsonrpc":"2.0","id":1,"result":{"name":"zechariah","address":"$ZECHARIAH_ADDRESS","txid":"ed98bed2e92ac46f16b27db5c55597539abc714fb259f1bc2b2fa5dfb586e266","height":3523662,"last_action":"claim","expires_at":"1824002006"}}
        """.trimIndent()
        private val STATUS_BODY = statusBody("\"${ZnsResolver.MAINNET_VIEWING_KEY}\"")

        private fun statusBody(viewingKey: String) =
            """{"jsonrpc":"2.0","id":1,"result":{"synced_height":3527683,"synced":true,"viewing_key":$viewingKey,"treasury_address":"u1f68wxdxsrswjl04kcc397a9ljedg88rl8dgazrk8l9v8jx6t3gqsktvqyvmc3ypgf4pum03q4ftvy5s0fe803nrxm8kw6dgzlvnv83nv"}}"""
    }
}
