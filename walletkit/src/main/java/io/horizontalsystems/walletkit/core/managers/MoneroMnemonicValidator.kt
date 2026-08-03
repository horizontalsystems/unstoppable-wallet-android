package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.monerokit.CakeWalletStyleConverter
import java.util.zip.CRC32

// Monero legacy (Electrum-style) 25-word mnemonic: 24 data words plus a checksum word,
// all from Monero's own 1626-word English list, which is disjoint from BIP39 wordlists.
object MoneroMnemonicValidator {

    const val WORD_COUNT = 25

    // Unique-prefix length of the English wordlist; the checksum is computed over
    // word prefixes, so it is a property of the wordlist, not a tunable.
    private const val PREFIX_LENGTH = 3

    private val wordSet: Set<String> by lazy { CakeWalletStyleConverter.MONERO_WORDLIST.toSet() }

    fun validWord(word: String, partial: Boolean): Boolean = if (partial) {
        CakeWalletStyleConverter.MONERO_WORDLIST.any { it.startsWith(word) }
    } else {
        word in wordSet
    }

    fun fetchSuggestions(prefix: String): List<String> {
        if (prefix.isEmpty()) return emptyList()
        return CakeWalletStyleConverter.MONERO_WORDLIST.filter { it.startsWith(prefix) }
    }

    @Throws(InvalidMoneroChecksumException::class)
    fun validateChecksum(words: List<String>) {
        require(words.size == WORD_COUNT) { "Monero mnemonic must be $WORD_COUNT words long" }

        val dataWords = words.take(WORD_COUNT - 1)
        val checksumWord = words.last()

        val crc = CRC32()
        crc.update(dataWords.joinToString("") { it.take(PREFIX_LENGTH) }.toByteArray(Charsets.UTF_8))
        val expectedWord = dataWords[(crc.value % dataWords.size).toInt()]

        if (expectedWord != checksumWord) {
            throw InvalidMoneroChecksumException()
        }
    }

    class InvalidMoneroChecksumException : Exception("Invalid Monero mnemonic checksum")
}
