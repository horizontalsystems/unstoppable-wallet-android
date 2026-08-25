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
        val key = BigInteger(1, ByteArray(32) { 0xff.toByte() })
        assertEquals("f".repeat(64), privateKeyHex(key))
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
}
