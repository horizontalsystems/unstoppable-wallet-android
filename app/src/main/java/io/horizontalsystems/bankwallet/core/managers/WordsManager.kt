package io.horizontalsystems.bankwallet.core.managers

import android.security.keystore.UserNotAuthenticatedException
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ISecuredStorage
import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.bankwallet.core.LogInState
import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class WordsManager(private val localStorage: ILocalStorage, private val secureStorage: ISecuredStorage) : IWordsManager {

    override val words: List<String>?
        get() = subject.value?.words

    private val subject = BehaviorSubject.create<AuthData>()
    override val authDataObservable: Flowable<AuthData> = subject.toFlowable(BackpressureStrategy.DROP)

    @Throws(UserNotAuthenticatedException::class)
    override fun safeLoad() {
        secureStorage.authData?.let {
            subject.onNext(it)
        }
    }

    @Throws(UserNotAuthenticatedException::class)
    override fun createWords() {
        AuthData().let {
            subject.onNext(it)
            secureStorage.saveAuthData(it)
        }
        localStorage.isNewWallet = true
    }

    @Throws(UserNotAuthenticatedException::class)
    override fun restore(words: List<String>) {
        AuthData(words).let {
            subject.onNext(it)
            secureStorage.saveAuthData(it)
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
        localStorage.clearAll()

        loggedInSubject.onNext(LogInState.LOGOUT)
    }

}
