package io.horizontalsystems.bankwallet.modules.restore.words

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.bankwallet.core.managers.ZcashBirthdayProvider
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsModule.RestoreAccountType

class RestoreWordsService(
        private val restoreAccountType: RestoreAccountType,
        private val wordsManager: IWordsManager,
        private val zcashBirthdayProvider: ZcashBirthdayProvider
) : RestoreWordsModule.IRestoreWordsService, Clearable {

    override val wordCount: Int
        get() = restoreAccountType.wordsCount

    override val hasAdditionalInfo: Boolean
        get() = restoreAccountType == RestoreAccountType.ZCASH

    override fun accountType(words: List<String>, additionalInfo: String?): AccountType {
        wordsManager.validate(words, wordCount)
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

    override fun clear() {}

    open class RestoreWordsException : Throwable() {
        class InvalidBirthdayHeightException : RestoreWordsException()
    }
}
