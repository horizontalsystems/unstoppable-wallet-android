package bitcoin.wallet.modules.backup

import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.core.IRandomProvider
import bitcoin.wallet.core.managers.WordsManager
import java.util.*

class BackupInteractor(private val wordsManager: WordsManager, private val indexesProvider: IRandomProvider, private val keystoreSafeExecute: IKeyStoreSafeExecute) : BackupModule.IInteractor {

    var delegate: BackupModule.IInteractorDelegate? = null

    override fun fetchWords() {
        keystoreSafeExecute.safeExecute(
                action = Runnable {
                    wordsManager.savedWords()?.let {
                        delegate?.didFetchWords(it)
                    }
                }
        )
    }

    override fun fetchConfirmationIndexes() {
        delegate?.didFetchConfirmationIndexes(indexesProvider.getRandomIndexes(2))
    }

    override fun validate(confirmationWords: HashMap<Int, String>) {
        keystoreSafeExecute.safeExecute(
                action = Runnable {
                    wordsManager.savedWords()?.let { wordList ->
                        for ((index, word) in confirmationWords) {
                            if (wordList[index - 1] != word.trim()) {
                                throw Exception()
                            }
                        }
                        wordsManager.wordListBackedUp = true
                    } ?: run { throw Exception() }
                },
                onSuccess = Runnable { delegate?.didValidateSuccess() },
                onFailure = Runnable { delegate?.didValidateFailure() }
        )
    }

}
