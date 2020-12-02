package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.bankwallet.core.InvalidMnemonicWordsCountException
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.hdwalletkit.WordList
import kotlin.jvm.Throws

class WordsManager : IWordsManager {
    private val wordList = WordList.getWords()
    private val wordsString = wordList.joinToString(" ")

    @Throws
    override fun validate(words: List<String>, wordCount: Int) {
        if (words.size != wordCount) {
            throw InvalidMnemonicWordsCountException()
        }

        Mnemonic().validate(words)
    }

    override fun wordExists(word: String): Boolean {
        return wordList.binarySearch(word) >= 0
    }

    override fun wordContains(word: String): Boolean {
        return wordsString.contains(word)
    }

    override fun generateWords(count: Int): List<String> {
        val strength = when (count) {
            24 -> Mnemonic.Strength.VeryHigh
            else -> Mnemonic.Strength.Default
        }

        return Mnemonic().generate(strength)
    }

}
