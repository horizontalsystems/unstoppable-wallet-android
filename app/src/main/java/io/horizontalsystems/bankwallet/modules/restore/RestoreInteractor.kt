package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.hdwalletkit.Mnemonic

class RestoreInteractor(private val wordsManager: WordsManager) : RestoreModule.IInteractor {

    var delegate: RestoreModule.IInteractorDelegate? = null

    override fun validate(words: List<String>) {
        try {
            wordsManager.validate(words)
            delegate?.didValidate(words)
        } catch (e: Mnemonic.MnemonicException) {
            delegate?.didFailToValidate(e)
        }
    }

}
