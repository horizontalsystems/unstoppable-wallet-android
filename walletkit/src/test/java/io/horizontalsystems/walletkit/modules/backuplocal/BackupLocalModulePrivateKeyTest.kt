package io.horizontalsystems.walletkit.modules.backuplocal

import io.horizontalsystems.walletkit.core.toPrivateKeyBytes
import io.horizontalsystems.walletkit.core.toRawHexString
import io.horizontalsystems.walletkit.entities.AccountType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class BackupLocalModulePrivateKeyTest {

    // high bit set: BigInteger.toByteArray() gives 33 bytes with a 0x00 sign byte
    private val highBitKey = "f1e2d3c4b5a69788796a5b4c3d2e1f00112233445566778899aabbccddeeff01"

    // leading zero byte: BigInteger.toByteArray() gives 31 bytes
    private val leadingZeroKey = "00a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f"

    private val regularKey = "4c0883a69102937d6231471b5dbb6204fe5129617082792ae468d01a3f362318"

    @Test
    fun evmPrivateKey_isWrittenAsRaw32Bytes() {
        listOf(highBitKey, leadingZeroKey, regularKey).forEach { hex ->
            val data = BackupLocalModule.getDataForEncryption(AccountType.EvmPrivateKey(BigInteger(hex, 16)))

            assertArrayEquals(hex, hex.hexToBytes(), data)
        }
    }

    @Test
    fun tronPrivateKey_isWrittenAsRaw32Bytes() {
        listOf(highBitKey, leadingZeroKey, regularKey).forEach { hex ->
            val data = BackupLocalModule.getDataForEncryption(AccountType.TronPrivateKey(BigInteger(hex, 16)))

            assertArrayEquals(hex, hex.hexToBytes(), data)
        }
    }

    @Test
    fun evmPrivateKey_roundTrips() {
        listOf(highBitKey, leadingZeroKey, regularKey).forEach { hex ->
            val accountType = AccountType.EvmPrivateKey(BigInteger(hex, 16))
            val type = BackupLocalModule.getAccountTypeString(accountType)
            val data = BackupLocalModule.getDataForEncryption(accountType)

            assertEquals(hex, accountType, BackupLocalModule.getAccountTypeFromData(type, data))
        }
    }

    @Test
    fun tronPrivateKey_roundTrips() {
        listOf(highBitKey, leadingZeroKey, regularKey).forEach { hex ->
            val accountType = AccountType.TronPrivateKey(BigInteger(hex, 16))
            val type = BackupLocalModule.getAccountTypeString(accountType)
            val data = BackupLocalModule.getDataForEncryption(accountType)

            assertEquals(hex, accountType, BackupLocalModule.getAccountTypeFromData(type, data))
        }
    }

    @Test
    fun evmPrivateKey_restoresLegacyAndroidBackups() {
        // backups written before the fix hold BigInteger.toByteArray() output
        listOf(highBitKey, leadingZeroKey, regularKey).forEach { hex ->
            val key = BigInteger(hex, 16)
            val accountType = BackupLocalModule.getAccountTypeFromData("private_key", key.toByteArray())

            assertEquals(hex, AccountType.EvmPrivateKey(key), accountType)
        }
    }

    @Test
    fun privateKeyHex_isAlways64Chars() {
        listOf(highBitKey, leadingZeroKey, regularKey).forEach { hex ->
            assertEquals(hex, BigInteger(hex, 16).toPrivateKeyBytes().toRawHexString())
        }
    }

    private fun String.hexToBytes(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
