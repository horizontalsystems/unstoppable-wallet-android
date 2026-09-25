package io.horizontalsystems.walletkit.chain.near

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.modules.address.AddressHandlerNear
import io.horizontalsystems.walletkit.modules.addtoken.AddNearTokenBlockchainService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NearAddressAndTokenTest {

    private val handler = AddressHandlerNear()
    private val addToken = AddNearTokenBlockchainService(Blockchain(BlockchainType.Near, "NEAR", null))

    @Test
    fun recognizesNearAccounts() {
        assertTrue(handler.isSupported("alice.near"))
        assertTrue(handler.isSupported("usdt.tether-token.near"))
        assertTrue(handler.isSupported("5510e2b44cae6eb807e3e0e45d579dda058c274abcba15e5cb84636f5d1ee412"))
        assertTrue(handler.isSupported("0x1234567890abcdef1234567890abcdef12345678"))
    }

    @Test
    fun leavesOtherInputAlone() {
        // single-label names are valid ids but would claim any lowercase word
        assertFalse(handler.isSupported("alice"))
        assertFalse(handler.isSupported("Alice.near"))
        assertFalse(handler.isSupported("GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN"))
        assertFalse(handler.isSupported("rMxCKbEDwqr76QuheSUMdEGf4B9xJ8m5De"))
        assertFalse(handler.isSupported("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"))
    }

    @Test
    fun parsesTokenReferences() {
        assertEquals(TokenType.Nep141("usdt.tether-token.near"), addToken.tokenQuery("usdt.tether-token.near").tokenType)
        assertEquals(TokenType.Nep141("wrap.near"), addToken.tokenQuery("  Wrap.Near ").tokenType)
        assertEquals(TokenType.Nep141("usdt.tether-token.near"), addToken.tokenQuery("https://nearblocks.io/token/usdt.tether-token.near?tab=holders").tokenType)
        assertFalse(addToken.isValid("not a contract"))
        assertFalse(addToken.isValid(""))
    }
}
