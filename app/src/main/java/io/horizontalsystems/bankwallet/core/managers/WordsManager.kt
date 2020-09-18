package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.bankwallet.core.InvalidMnemonicWordsCountException
import io.horizontalsystems.hdwalletkit.Mnemonic

class WordsManager : IWordsManager {

    @Throws()
    override fun validate(words: List<String>, wordCount: Int) {
        if (words.size != wordCount) {
            throw InvalidMnemonicWordsCountException()
        }
        Mnemonic().validate(words)
    }

    override fun generateWords(count: Int): List<String> {
        val strength = when (count) {
            24 -> Mnemonic.Strength.VeryHigh
            else -> Mnemonic.Strength.Default
        }

        return Mnemonic().generate(strength)
    }

}
