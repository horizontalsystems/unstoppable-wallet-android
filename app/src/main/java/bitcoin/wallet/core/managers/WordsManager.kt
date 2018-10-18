package bitcoin.wallet.core.managers

import android.security.keystore.UserNotAuthenticatedException
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.ISecuredStorage
import bitcoin.wallet.core.IWordsManager
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.subjects.PublishSubject

class WordsManager(private val localStorage: ILocalStorage, private val secureStorage: ISecuredStorage): IWordsManager {

    override var words: List<String>? = null

    @Throws(UserNotAuthenticatedException::class)
    override fun safeLoad() {
        words = secureStorage.savedWords
    }

    @Throws(UserNotAuthenticatedException::class)
    override fun createWords() {
        words = Mnemonic().generate()
        words?.let {
            secureStorage.saveWords(it)
        }
    }

    @Throws(Mnemonic.MnemonicException::class, UserNotAuthenticatedException::class)
    override fun restore(words: List<String>) {
        Mnemonic().validate(words)
        secureStorage.saveWords(words)
        this.words = words
    }

    override var isBackedUp: Boolean
        get() {
            return localStorage.isBackedUp
        }
        set(value) {
            localStorage.isBackedUp = value
            backedUpSubject.onNext(value)
        }

    override var isLoggedIn: Boolean = false
        get() = !secureStorage.wordsAreEmpty()

    override var backedUpSubject: PublishSubject<Boolean> = PublishSubject.create()

    override fun validate(words: List<String>) {
        Mnemonic().validate(words)
    }

    override fun removeWords() {
        words = null
        secureStorage.saveWords(listOf())
    }

}
