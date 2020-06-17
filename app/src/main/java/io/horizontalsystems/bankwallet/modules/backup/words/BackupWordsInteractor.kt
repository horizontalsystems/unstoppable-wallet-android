package io.horizontalsystems.bankwallet.modules.backup.words

import io.horizontalsystems.bankwallet.core.IRandomProvider
import java.util.*

class BackupWordsInteractor(private val indexesProvider: IRandomProvider, private val words: Array<String>)
    : BackupWordsModule.IInteractor {

    var delegate: BackupWordsModule.IInteractorDelegate? = null

    override fun getConfirmationIndices(maxIndex: Int): List<Int> {
        return indexesProvider.getRandomIndexes(2, maxIndex)
    }

    override fun validate(confirmationWords: HashMap<Int, String>) {
        for ((index, word) in confirmationWords) {
            if (words[index - 1] != word.trim()) {
                delegate?.onValidateFailure()
                return
            }
        }

        delegate?.onValidateSuccess()
    }
}
