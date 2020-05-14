package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.hdwalletkit.Mnemonic

class WordsManager : IWordsManager {

    @Throws(Mnemonic.MnemonicException::class)
    override fun validate(words: List<String>) {
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
