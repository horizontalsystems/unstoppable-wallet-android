package io.horizontalsystems.bankwallet.modules.restore.words

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.bankwallet.entities.AccountType

class RestoreWordsService(
        val wordCount: Int,
        private val wordsManager: IWordsManager
): RestoreWordsModule.IRestoreWordsService, Clearable {

    override fun accountType(words: List<String>): AccountType {
        wordsManager.validate(words, wordCount)
        return AccountType.Mnemonic(words, null)
    }

    override fun clear() {

    }
}
