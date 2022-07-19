package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.hdwalletkit.Mnemonic

class WordsManager(
    private val mnemonic: Mnemonic
) : IWordsManager {

    @Throws
    override fun validateChecksum(words: List<String>) {
        mnemonic.validate(words)
    }

    override fun isWordValid(word: String): Boolean {
        return mnemonic.isWordValid(word)
    }

    override fun isWordPartiallyValid(word: String): Boolean {
        return mnemonic.isWordValid(word, partial = true)
    }

    override fun generateWords(count: Int): List<String> {
        val strength = when (count) {
            24 -> Mnemonic.EntropyStrength.VeryHigh
            else -> Mnemonic.EntropyStrength.Default
        }

        return mnemonic.generate(strength)
    }
}
