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

    private val words: List<String>
    private val passphrase: String

    var firstWord: String = ""
    var secondWord: String = ""
    var passphraseConfirm: String = ""

    init {
        if (account.type is AccountType.Mnemonic) {
            words = account.type.words
            passphrase = account.type.passphrase
        } else {
            words = listOf()
            passphrase = ""
        }
    }

    private fun validate(): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (firstWord.isBlank()) {
            errors.add(ValidationError.EmptyFirstWord)
        } else if (firstWord != words[indexItem.first]) {
            errors.add(ValidationError.InvalidFirstWord)
        }

        if (secondWord.isBlank()) {
            errors.add(ValidationError.EmptySecondWord)
        } else if (secondWord != words[indexItem.second]) {
            errors.add(ValidationError.InvalidSecondWord)
        }

        if (passphrase.isNotBlank()) {
            if (passphraseConfirm.isBlank()) {
                errors.add(ValidationError.EmptyPassphrase)
            } else if (passphraseConfirm != passphrase) {
                errors.add(ValidationError.InvalidPassphrase)
            }
        }

        return errors
    }

    fun generateIndices() {
        val indices = indexesProvider.getRandomIndexes(2, words.size)
        indexItem = IndexItem(indices[0], indices[1])
    }

    fun hasPassphrase(): Boolean {
        return passphrase.isNotBlank()
    }

    fun backup() {
        val validationErrors = validate()

        if (validationErrors.isNotEmpty()) {
            throw BackupError(validationErrors)
        }

        account.isBackedUp = true
        accountManager.update(account)
    }

    data class IndexItem(val first: Int, val second: Int)

    class BackupError(val validationErrors: List<ValidationError>) : Exception()

    sealed class ValidationError : Exception() {
        object EmptyFirstWord : ValidationError()
        object InvalidFirstWord : ValidationError()
        object EmptySecondWord : ValidationError()
        object InvalidSecondWord : ValidationError()
        object EmptyPassphrase : ValidationError()
        object InvalidPassphrase : ValidationError()
    }

}
