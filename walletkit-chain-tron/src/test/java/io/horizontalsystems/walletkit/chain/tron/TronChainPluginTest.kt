package io.horizontalsystems.walletkit.chain.tron

import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.modules.manageaccount.evmprivatekey.PrivateKeyPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class TronChainPluginTest {

    private fun privateKeyRows(key: BigInteger) = TronChainPlugin().privateKeyRows(
        Account("", "test", AccountType.TronPrivateKey(key), AccountOrigin.Restored, 0)
    )

    private fun privateKeyHex(key: BigInteger): String {
        val row = privateKeyRows(key).single()
        return (row.page as PrivateKeyPage).input.privateKey
    }

    @Test
    fun privateKeyHexIsPaddedTo64Chars() {
        assertEquals("0".repeat(63) + "1", privateKeyHex(BigInteger.ONE))
    }

    @Test
    fun signByteIsStrippedFrom33ByteKeys() {
        // largest valid key; its high bit forces a sign byte in BigInteger.toByteArray()
        val key = secp256k1Order.subtract(BigInteger.ONE)
        assertEquals(
            "fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364140",
            privateKeyHex(key)
        )
    }

    @Test
    fun overWideKeyRendersNoRowInsteadOfTruncating() {
        assertTrue(privateKeyRows(BigInteger.ONE.shiftLeft(256)).isEmpty())
    }

    @Test
    fun negativeKeyRendersNoRowInsteadOfReinterpreting() {
        assertTrue(privateKeyRows(BigInteger.ONE.negate()).isEmpty())
    }

    private fun publicKeyRows(key: BigInteger) = TronChainPlugin().publicKeyRows(
        Account("", "test", AccountType.TronPrivateKey(key), AccountOrigin.Restored, 0)
    )

    @Test
    fun overWideKeyRendersNoAddressRow() {
        assertTrue(publicKeyRows(BigInteger.ONE.shiftLeft(256)).isEmpty())
    }

    @Test
    fun negativeKeyRendersNoAddressRow() {
        assertTrue(publicKeyRows(BigInteger.ONE.negate()).isEmpty())
    }

    private val secp256k1Order = BigInteger(
        "fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141",
        16
    )

    @Test
    fun keysOutsideScalarRangeRenderNoRows() {
        for (key in listOf(BigInteger.ZERO, secp256k1Order, secp256k1Order.plus(BigInteger.ONE))) {
            assertTrue(privateKeyRows(key).isEmpty())
            assertTrue(publicKeyRows(key).isEmpty())
        }
    }
}
