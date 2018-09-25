package bitcoin.wallet.modules.backup

import bitcoin.wallet.core.IRandomProvider
import bitcoin.wallet.core.managers.WordsManager
import java.util.*

class BackupInteractor(private val wordsManager: WordsManager, private val indexesProvider: IRandomProvider) : BackupModule.IInteractor {

    var delegate: BackupModule.IInteractorDelegate? = null

    override fun fetchWords() {
        wordsManager.savedWords?.let {
            delegate?.didFetchWords(it)
        }
    }

    override fun fetchConfirmationIndexes() {
        delegate?.didFetchConfirmationIndexes(indexesProvider.getRandomIndexes(2))
    }

    override fun validate(confirmationWords: HashMap<Int, String>) {
        wordsManager.savedWords?.let { wordList ->
            for ((index, word) in confirmationWords) {
                if (wordList[index - 1] != word.trim()) {
                    delegate?.didValidateFailure()
                    return
                }
            }
            wordsManager.wordListBackedUp = true
            delegate?.didValidateSuccess()
        } ?: run { delegate?.didValidateFailure() }
    }

}
