package io.horizontalsystems.bankwallet.modules.restore.words

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.bankwallet.core.managers.ZcashBirthdayProvider
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsModule.RestoreAccountType
import io.horizontalsystems.hdwalletkit.Mnemonic

class RestoreWordsService(
        private val restoreAccountType: RestoreAccountType,
        private val wordsManager: IWordsManager,
        private val zcashBirthdayProvider: ZcashBirthdayProvider)
    : RestoreWordsModule.IRestoreWordsService, Clearable {

    override val wordCount: Int
        get() = restoreAccountType.wordsCount

    override val birthdayHeightEnabled: Boolean
        get() = restoreAccountType == RestoreAccountType.ZCASH

    override fun accountType(words: List<String>, additionalInfo: String?): AccountType {
        words.forEach {
            if (!isWordValid(it)) {
                throw RestoreWordsException.InvalidWordException(it)
            }
        }

        if (words.size != wordCount) {
            throw RestoreWordsException.InvalidWordCountException(words.size, wordCount)
        }

        try {
            wordsManager.validateChecksum(words)
        } catch (e: Mnemonic.ChecksumException){
            throw RestoreWordsException.ChecksumException()
        }

        return when (restoreAccountType) {
            RestoreAccountType.STANDARD,
            RestoreAccountType.BINANCE -> AccountType.Mnemonic(words)
            RestoreAccountType.ZCASH -> {
                val birthdayHeight = if (additionalInfo.isNullOrBlank()) {
                    null
                } else {
                    try {
                        additionalInfo.toLong().apply {
                            zcashBirthdayProvider.validateBirthdayHeight(this)
                        }
                    } catch (error: Throwable) {
                        throw RestoreWordsException.InvalidBirthdayHeightException()
                    }
                }
                AccountType.Zcash(words, birthdayHeight)
            }
        }
    }

    override fun isWordValid(word: String): Boolean {
        return wordsManager.isWordValid(word)
    }

    override fun isWordPartiallyValid(word: String): Boolean {
        return wordsManager.isWordPartiallyValid(word)
    }

    override fun clear() {}

    open class RestoreWordsException : Throwable() {
        class InvalidBirthdayHeightException : RestoreWordsException()
        class ChecksumException : RestoreWordsException()
        class InvalidWordException(val word: String) : RestoreWordsException()
        class InvalidWordCountException(val count: Int, val requiredCount: Int) : RestoreWordsException()
    }
}
