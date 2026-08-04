package io.horizontalsystems.walletkit.chain.zano

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZanoAliasTest {

    @Test
    fun normalize_validAliases() {
        assertEquals("zano", AddressHandlerZanoAlias.normalize("@zano"))
        assertEquals("gigabyted", AddressHandlerZanoAlias.normalize("gigabyted"))
        assertEquals("gigabyted", AddressHandlerZanoAlias.normalize("@gigabyted"))
        assertEquals("alias123", AddressHandlerZanoAlias.normalize("alias123"))
        assertEquals("a", AddressHandlerZanoAlias.normalize("@a"))
        assertEquals("a".repeat(25), AddressHandlerZanoAlias.normalize("a".repeat(25)))
        // dots and hyphens are part of the on-chain alias charset
        assertEquals("-axel-", AddressHandlerZanoAlias.normalize("-axel-"))
        assertEquals("---007", AddressHandlerZanoAlias.normalize("@---007"))
        assertEquals("some.name", AddressHandlerZanoAlias.normalize("some.name"))
    }

    @Test
    fun normalize_invalidAliases() {
        assertNull(AddressHandlerZanoAlias.normalize("a"))
        assertNull(AddressHandlerZanoAlias.normalize(""))
        assertNull(AddressHandlerZanoAlias.normalize("@"))
        assertNull(AddressHandlerZanoAlias.normalize("MyName"))
        assertNull(AddressHandlerZanoAlias.normalize("with space"))
        assertNull(AddressHandlerZanoAlias.normalize("under_score"))
        assertNull(AddressHandlerZanoAlias.normalize("a".repeat(26)))
        assertNull(AddressHandlerZanoAlias.normalize("@@double"))
        assertNull(
            AddressHandlerZanoAlias.normalize(
                "ZxDGngsbdEvaPWoYmV995cKddBqYn1A963Wu2xRJUotE65J9FzitMtLAeYKwQewEGYVLsoc1MqRKghhGCmFEpcPo2BMnzYeCJ"
            )
        )
    }

    @Test
    fun parseAliasAddress_ok() {
        val body = """
            {
              "id": 0,
              "jsonrpc": "2.0",
              "result": {
                "alias_details": {
                  "address": "ZxDGngsbdEvaPWoYmV995cKddBqYn1A963Wu2xRJUotE65J9FzitMtLAeYKwQewEGYVLsoc1MqRKghhGCmFEpcPo2BMnzYeCJ",
                  "comment": "the one and only!",
                  "tracking_key": ""
                },
                "status": "OK"
              }
            }
        """.trimIndent()

        assertEquals(
            "ZxDGngsbdEvaPWoYmV995cKddBqYn1A963Wu2xRJUotE65J9FzitMtLAeYKwQewEGYVLsoc1MqRKghhGCmFEpcPo2BMnzYeCJ",
            ZanoAliasResolver.parseAliasAddress(body)
        )
    }

    @Test
    fun parseAliasAddress_error() {
        val body = """{"id":0,"jsonrpc":"2.0","error":{"code":-1,"message":"Unknown alias"}}"""
        assertNull(ZanoAliasResolver.parseAliasAddress(body))
    }

    @Test
    fun parseAliasAddress_notFound() {
        // actual shape returned by public nodes for an unknown alias
        val body = """
            {"id":0,"jsonrpc":"2.0","result":{"alias_details":{"address":"","comment":"","tracking_key":""},"status":"NOT_FOUND"}}
        """.trimIndent()
        assertNull(ZanoAliasResolver.parseAliasAddress(body))
    }

    @Test
    fun parseAliasAddress_okWithEmptyAddress() {
        val body = """{"id":0,"jsonrpc":"2.0","result":{"alias_details":{"address":""},"status":"OK"}}"""
        assertNull(ZanoAliasResolver.parseAliasAddress(body))
    }

    @Test
    fun parseAliasAddress_missingAliasDetails() {
        val body = """{"id":0,"jsonrpc":"2.0","result":{"status":"OK"}}"""
        assertNull(ZanoAliasResolver.parseAliasAddress(body))
    }

    @Test
    fun parseAliasAddress_malformedJson() {
        assertNull(ZanoAliasResolver.parseAliasAddress("not a json"))
        assertNull(ZanoAliasResolver.parseAliasAddress(""))
        assertNull(ZanoAliasResolver.parseAliasAddress("[]"))
    }
}
