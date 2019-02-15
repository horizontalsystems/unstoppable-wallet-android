package io.horizontalsystems.bankwallet.entities

import org.junit.Assert
import org.junit.Test

class AuthDataTest {

    @Test
    fun serializeAndDeserialize() {
        val words = listOf("include", "kit", "trade", "lion", "lunar", "silly", "attract", "artwork", "vapor", "chest", "unable", "link")
        val authData = AuthData(words)

        val serialized = authData.toString()

        val parsedFromString = AuthData(serialized)

        Assert.assertArrayEquals(words.toTypedArray(), parsedFromString.words.toTypedArray())
        Assert.assertArrayEquals(authData.seed, parsedFromString.seed)
        Assert.assertEquals(authData.walletId, parsedFromString.walletId)
    }

}
