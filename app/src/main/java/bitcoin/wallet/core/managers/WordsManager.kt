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
            storage.wordlistBackedUp(value)
            wordListBackedUpSubject.onNext(value)
        }

    var savedWords: List<String>? = storage.savedWords

    @Throws(UserNotAuthenticatedException::class)
    fun createWords(): List<String> {
        val generatedWords = Mnemonic().generate()
        storage.saveWords(generatedWords)
        return generatedWords
    }

    fun restore(words: List<String>) {
        Mnemonic().validate(words)
        storage.saveWords(words)
    }

}
