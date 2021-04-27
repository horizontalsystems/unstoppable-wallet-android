package io.horizontalsystems.bankwallet.modules.restoremnemonic

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.bankwallet.entities.AccountType
import io.reactivex.subjects.BehaviorSubject

class RestoreMnemonicService(private val wordsManager: WordsManager) : Clearable {

    var passphraseEnabled: Boolean = false
        set(value) {
            field = value
            passphraseEnabledObservable.onNext(value)
        }
    val passphraseEnabledObservable = BehaviorSubject.createDefault(passphraseEnabled)

    var passphrase = ""

    override fun clear() = Unit

    fun isWordValid(word: String): Boolean {
        return wordsManager.isWordValid(word)
    }

    fun isWordPartiallyValid(word: String): Boolean {
        return wordsManager.isWordPartiallyValid(word)
    }

    fun accountType(words: List<String>): AccountType {
        if (passphraseEnabled && passphrase.isBlank()) {
            throw RestoreError.EmptyPassphrase
        }

        if (words.size != 12 && words.size != 24) {
            throw ValidationError.InvalidWordCountException(words.size)
        }

        wordsManager.validateChecksum(words)

        return AccountType.Mnemonic(words, passphrase)
    }

    sealed class ValidationError : Throwable() {
        class InvalidWordCountException(val count: Int) : ValidationError()
    }

    sealed class RestoreError : Throwable() {
        object EmptyPassphrase : RestoreError()
    }

}
