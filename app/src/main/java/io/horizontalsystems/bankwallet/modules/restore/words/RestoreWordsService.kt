package io.horizontalsystems.bankwallet.modules.restore.words

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsModule.RestoreAccountType

class RestoreWordsService(
        private val restoreAccountType: RestoreAccountType,
        private val wordsManager: IWordsManager
) : RestoreWordsModule.IRestoreWordsService, Clearable {

    override val wordCount: Int
        get() = restoreAccountType.wordsCount

    override fun accountType(words: List<String>): AccountType {
        wordsManager.validate(words, wordCount)
        return when (restoreAccountType) {
            RestoreAccountType.STANDARD,
            RestoreAccountType.BINANCE -> AccountType.Mnemonic(words)
            RestoreAccountType.ZCASH -> AccountType.Zcash(words)
        }
    }

    override fun clear() {}

}
