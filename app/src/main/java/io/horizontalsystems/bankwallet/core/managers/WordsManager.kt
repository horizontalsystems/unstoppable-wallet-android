package io.horizontalsystems.bankwallet.core.managers

import android.security.keystore.UserNotAuthenticatedException
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ISecuredStorage
import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.bankwallet.core.LogInState
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*

class WordsManager(private val localStorage: ILocalStorage, private val secureStorage: ISecuredStorage) : IWordsManager {

    override var words: List<String>? = null
        set(value) {
            field = value
            subject.onNext(value ?: listOf())
        }
    private val subject = BehaviorSubject.createDefault(words ?: listOf())
    override val wordsObservable: Flowable<List<String>> = subject.toFlowable(BackpressureStrategy.DROP)
    override var walletId: String? = null

    @Throws(UserNotAuthenticatedException::class)
    override fun safeLoad() {
        val savedWords = secureStorage.authData
        words = savedWords?.subList(0, 12)
        walletId = savedWords?.getOrNull(12)
    }

    @Throws(UserNotAuthenticatedException::class)
    override fun createWords() {
        words = Mnemonic().generate()
        words?.let {
            saveWords(it)
        }
        localStorage.isNewWallet = true
    }

    @Throws(UserNotAuthenticatedException::class)
    override fun restore(words: List<String>) {
        saveWords(words)
        this.words = words
    }

    private fun saveWords(words: List<String>) {
        UUID.randomUUID().toString().let { it ->
            walletId = it
            secureStorage.saveAuthData(words.plus(it))
        }
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
        get() = !secureStorage.noAuthData()

    override var loggedInSubject: PublishSubject<LogInState> = PublishSubject.create()

    override var backedUpSubject: PublishSubject<Boolean> = PublishSubject.create()

    @Throws(Mnemonic.MnemonicException::class)
    override fun validate(words: List<String>) {
        Mnemonic().validate(words)
    }

    override fun logout() {
        words = null
        localStorage.clearAll()

        loggedInSubject.onNext(LogInState.LOGOUT)
    }

}
