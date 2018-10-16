package bitcoin.wallet.core.managers

import android.security.keystore.UserNotAuthenticatedException
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.kit.hdwallet.Mnemonic
import io.reactivex.subjects.PublishSubject

class WordsManager(private val storage: ILocalStorage) {

    var wordListBackedUpSubject: PublishSubject<Boolean> = PublishSubject.create()

    var wordListBackedUp: Boolean
        get() {
            return storage.isWordListBackedUp()
        }
        set(value) {
            storage.wordListBackedUp(value)
            wordListBackedUpSubject.onNext(value)
        }

    fun savedWords(): List<String>? = storage.savedWords
    fun wordsAreEmpty(): Boolean = storage.wordsAreEmpty()

    @Throws(UserNotAuthenticatedException::class)
    fun createWords(): List<String> {
        val generatedWords = Mnemonic().generate()
        storage.saveWords(generatedWords)
        return generatedWords
    }

    @Throws(Mnemonic.MnemonicException::class, UserNotAuthenticatedException::class)
    fun restore(words: List<String>) {
        Mnemonic().validate(words)
        storage.saveWords(words)
    }

    fun clear() {
        storage.saveWords(listOf())
    }

}
