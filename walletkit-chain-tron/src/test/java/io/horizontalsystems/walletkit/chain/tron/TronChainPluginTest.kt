package io.horizontalsystems.walletkit.chain.tron

import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.modules.manageaccount.evmprivatekey.PrivateKeyPage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class TronChainPluginTest {

    private fun privateKeyHex(key: BigInteger): String {
        val account = Account("", "test", AccountType.TronPrivateKey(key), AccountOrigin.Restored, 0)
        val row = TronChainPlugin().privateKeyRows(account).single()
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
}
