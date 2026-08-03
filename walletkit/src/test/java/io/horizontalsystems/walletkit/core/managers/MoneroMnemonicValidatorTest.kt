package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.monerokit.CakeWalletStyleConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneroMnemonicValidatorTest {

    // The kit's converter computes the checksum word with its own implementation while
    // encoding a BIP39 seed into the legacy format; the validator must agree with it.
    private val converterSeed = CakeWalletStyleConverter.getLegacySeedFromBip39(
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about".split(" ")
    )!!

    @Test
    fun `converter output passes checksum validation`() {
        assertEquals(MoneroMnemonicValidator.WORD_COUNT, converterSeed.size)
        MoneroMnemonicValidator.validateChecksum(converterSeed)
    }

    @Test(expected = MoneroMnemonicValidator.InvalidMoneroChecksumException::class)
    fun `tampered checksum word is rejected`() {
        val replacement = CakeWalletStyleConverter.MONERO_WORDLIST.first { it != converterSeed.last() }
        MoneroMnemonicValidator.validateChecksum(converterSeed.dropLast(1) + replacement)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `wrong word count is rejected`() {
        MoneroMnemonicValidator.validateChecksum(converterSeed.dropLast(1))
    }

    @Test
    fun `word validity checks against the Monero wordlist`() {
        assertTrue(MoneroMnemonicValidator.validWord("abbey", partial = false))
        assertFalse(MoneroMnemonicValidator.validWord("abandon", partial = false)) // BIP39-only word
        assertTrue(MoneroMnemonicValidator.validWord("abb", partial = true))
        assertFalse(MoneroMnemonicValidator.validWord("zzz", partial = true))
    }

    @Test
    fun `suggestions are prefix matches`() {
        val suggestions = MoneroMnemonicValidator.fetchSuggestions("abb")
        assertTrue(suggestions.contains("abbey"))
        assertTrue(suggestions.all { it.startsWith("abb") })
        assertTrue(MoneroMnemonicValidator.fetchSuggestions("").isEmpty())
    }
}
