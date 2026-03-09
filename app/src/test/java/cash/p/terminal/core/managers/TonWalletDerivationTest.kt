package cash.p.terminal.core.managers

import cash.p.terminal.core.storage.HardwarePublicKeyStorage
import cash.p.terminal.core.toFixedSize
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import io.horizontalsystems.tonkit.core.TonWallet
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class TonWalletDerivationTest {

    private val hardwarePublicKeyStorage: HardwarePublicKeyStorage = mockk()

    private fun createMnemonicAccount(words: List<String>): Account {
        return Account(
            id = "test",
            name = "test",
            type = AccountType.Mnemonic(words, ""),
            origin = AccountOrigin.Restored,
            level = 0
        )
    }

    @Test
    fun toTonWallet_mnemonicWith31BytePrivateKey_createsSeedSuccessfully() {
        // This mnemonic produces a 31-byte private key (leading 0x00 stripped by BigInteger)
        val words = "disease other myth twist flip law dice layer notice door staff require"
            .split(" ")

        val account = createMnemonicAccount(words)
        val tonWallet = account.toTonWallet(hardwarePublicKeyStorage, null)

        assert(tonWallet is TonWallet.Seed)
    }

    @Test
    fun toTonWallet_normalMnemonic_createsSeedSuccessfully() {
        val words = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
            .split(" ")

        val account = createMnemonicAccount(words)
        val tonWallet = account.toTonWallet(hardwarePublicKeyStorage, null)

        assert(tonWallet is TonWallet.Seed)
    }

    @Test
    fun toFixedSize_shortKey_padsWithLeadingZeros() {
        val shortKey = ByteArray(31) { (it + 1).toByte() }
        val result = shortKey.toFixedSize(32)

        assertEquals(32, result.size)
        assertEquals(0.toByte(), result[0])
        assertEquals(1.toByte(), result[1])
        assertEquals(31.toByte(), result[31])
    }

    @Test
    fun toFixedSize_longKey_stripsLeadingSignByte() {
        val longKey = ByteArray(33) { it.toByte() } // [0x00, 0x01, ..., 0x20]
        val result = longKey.toFixedSize(32)

        assertEquals(32, result.size)
        assertEquals(1.toByte(), result[0])
        assertEquals(32.toByte(), result[31])
    }

    @Test
    fun toFixedSize_exactSize_returnsUnchanged() {
        val key = ByteArray(32) { it.toByte() }
        val result = key.toFixedSize(32)

        assertEquals(32, result.size)
        assert(key.contentEquals(result))
    }
}
