package io.horizontalsystems.bankwallet.modules.backup

import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IRandomProvider
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import java.util.*

class BackupInteractor(
        private val wordsManager: WordsManager,
        private val indexesProvider: IRandomProvider,
        private val localStorage: ILocalStorage,
        private val keystoreSafeExecute: IKeyStoreSafeExecute) : BackupModule.IInteractor {

    var delegate: BackupModule.IInteractorDelegate? = null

    override fun fetchWords() {
        wordsManager.words?.let {
            delegate?.didFetchWords(it)
        } ?:run {
            keystoreSafeExecute.safeExecute(
                    action = Runnable {
                        wordsManager.safeLoad()
                        wordsManager.words?.let { delegate?.didFetchWords(it) }
                    }
            )
        }
    }

    override fun fetchConfirmationIndexes() {
        delegate?.didFetchConfirmationIndexes(indexesProvider.getRandomIndexes(2))
    }

    override fun validate(confirmationWords: HashMap<Int, String>) {
        wordsManager.words?.let { wordList ->
            var valid = true
            for ((index, word) in confirmationWords) {
                if (wordList[index - 1] != word.trim()) {
                    valid = false
                }
            }

            if (valid) {
                wordsManager.isBackedUp = true
                delegate?.didValidateSuccess()
            } else {
                delegate?.didValidateFailure()
            }
        } ?: run { delegate?.didValidateFailure() }
    }

    override fun onTermsConfirm() {
        localStorage.iUnderstand = true
    }
}
