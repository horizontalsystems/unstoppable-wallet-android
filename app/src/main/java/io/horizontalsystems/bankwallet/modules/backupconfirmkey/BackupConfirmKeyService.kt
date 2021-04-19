package io.horizontalsystems.bankwallet.modules.backupconfirmkey

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IRandomProvider
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.BehaviorSubject

class BackupConfirmKeyService(
        private val account: Account,
        private val accountManager: IAccountManager,
        private val indexesProvider: IRandomProvider
) {
    private val indexItemSubject = BehaviorSubject.create<IndexItem>()
    val indexItemObservable: Flowable<IndexItem> = indexItemSubject.toFlowable(BackpressureStrategy.BUFFER)

    var indexItem: IndexItem = IndexItem(0, 1)
        private set(value) {
            field = value
            indexItemSubject.onNext(value)
        }

    val words: List<String>
    val salt: String?
    init {
        if (account.type is AccountType.Mnemonic) {
            words = account.type.words
            salt = account.type.salt
        } else {
            words = listOf()
            salt = null
        }
    }

    private fun validate(word: String, validWord: String) {
        if (!word.trim().equals(validWord, ignoreCase = true)) {
            throw BackupValidationException()
        }
    }

    fun generateIndices() {
        val indices = indexesProvider.getRandomIndexes(2, words.size)
        indexItem = IndexItem(indices[0], indices[1])
    }

    fun backup(firstWord: String, secondWord: String) {
        validate(firstWord, words[indexItem.first])
        validate(secondWord, words[indexItem.second])

        account.isBackedUp = true
        accountManager.update(account)
    }

    data class IndexItem(val first: Int, val second: Int)

    class BackupValidationException : Exception()

}
