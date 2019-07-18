package io.horizontalsystems.bankwallet.modules.restore.words

import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.hdwalletkit.Mnemonic

class RestoreWordsInteractor(private val wordsManager: WordsManager) : RestoreWordsModule.IInteractor {

    var delegate: RestoreWordsModule.IInteractorDelegate? = null

    override fun validate(words: List<String>) {
        try {
            wordsManager.validate(words)
            delegate?.didValidate(words)
        } catch (e: Mnemonic.MnemonicException) {
            delegate?.didFailToValidate(e)
        }
    }

}
