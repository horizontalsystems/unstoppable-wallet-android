package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.hdwalletkit.WordList
import kotlin.jvm.Throws

class WordsManager : IWordsManager {
    private val wordList = WordList.getWords()

    @Throws
    override fun validateChecksum(words: List<String>) {
        Mnemonic().validateChecksum(words)
    }

    override fun isWordValid(word: String): Boolean {
        return wordList.binarySearch(word) >= 0
    }

    override fun isWordPartiallyValid(word: String): Boolean {
        return wordList.any { it.startsWith(word) }
    }

    override fun generateWords(count: Int): List<String> {
        val strength = when (count) {
            24 -> Mnemonic.Strength.VeryHigh
            else -> Mnemonic.Strength.Default
        }

        return Mnemonic().generate(strength)
    }
}
