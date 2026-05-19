package cash.p.terminal.wallet.entities

import kotlin.test.Test
import kotlin.test.assertEquals

class TokenTypeTest {

    @Test
    fun id_mweb_returnsMwebId() {
        assertEquals("mweb", TokenType.Mweb.id)
    }

    @Test
    fun values_mweb_returnsMwebTypeAndEmptyReference() {
        assertEquals(TokenType.Value(type = "mweb", reference = ""), TokenType.Mweb.values)
    }

    @Test
    fun fromType_mweb_returnsMwebTokenType() {
        assertEquals(TokenType.Mweb, TokenType.fromType("mweb"))
    }

    @Test
    fun fromId_mweb_returnsMwebTokenType() {
        assertEquals(TokenType.Mweb, TokenType.fromId("mweb"))
    }

    @Test
    fun fromType_stellarReference_returnsAssetTokenType() {
        assertEquals(
            TokenType.Asset(code = "USDC", issuer = "GDQOE23QKUSLOXFW77F5GBDSYFND5XPF7FJHJ42SRD7Y7RP2T7BDXYCP"),
            TokenType.fromType("stellar", "USDC-GDQOE23QKUSLOXFW77F5GBDSYFND5XPF7FJHJ42SRD7Y7RP2T7BDXYCP")
        )
    }

    @Test
    fun fromId_stellarAsset_returnsAssetTokenType() {
        val tokenType = TokenType.Asset(
            code = "USDC",
            issuer = "GDQOE23QKUSLOXFW77F5GBDSYFND5XPF7FJHJ42SRD7Y7RP2T7BDXYCP"
        )

        assertEquals(tokenType, TokenType.fromId(tokenType.id))
    }
}
