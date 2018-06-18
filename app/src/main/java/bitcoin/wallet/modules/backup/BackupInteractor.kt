package bitcoin.wallet.modules.backup

import bitcoin.wallet.core.IRandomProvider
import bitcoin.wallet.core.IWalletDataProvider
import java.util.*

class BackupInteractor(private val walletDataProvider: IWalletDataProvider, private val indexesProvider: IRandomProvider) : BackupModule.IInteractor {

    var delegate: BackupModule.IInteractorDelegate? = null

    override fun fetchWords() {
        delegate?.didFetchWords(walletDataProvider.walletData.words)
    }

    override fun fetchConfirmationIndexes() {
        delegate?.didFetchConfirmationIndexes(indexesProvider.getRandomIndexes(2))
    }

    override fun validate(confirmationWords: HashMap<Int, String>) {
        val words = walletDataProvider.walletData.words

        for ((index, word) in confirmationWords) {
            if (words[index - 1] != word.trim()) {
                delegate?.didValidateFailure()
                return
            }
        }

        delegate?.didValidateSuccess()
    }

}
