package io.horizontalsystems.bankwallet.modules.restoremnemonic

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.bankwallet.entities.AccountType

class RestoreMnemonicService(private val wordsManager: WordsManager) : Clearable {

    override fun clear() = Unit

    fun isWordValid(word: String): Boolean {
        return wordsManager.isWordValid(word)
    }

    fun isWordPartiallyValid(word: String): Boolean {
        return wordsManager.isWordPartiallyValid(word)
    }

    fun accountType(words: List<String>): AccountType {
        if (words.size != 12 && words.size != 24) {
            throw ValidationError.InvalidWordCountException(words.size)
        }

        wordsManager.validateChecksum(words)

        return AccountType.Mnemonic(words, null)
    }

    sealed class ValidationError : Throwable() {
        class InvalidWordCountException(val count: Int) : ValidationError()
    }
}
