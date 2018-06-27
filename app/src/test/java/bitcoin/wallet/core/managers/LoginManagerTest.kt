package bitcoin.wallet.core.managers

import bitcoin.wallet.WalletManager
import bitcoin.wallet.WalletWrapper
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.INetworkManager
import bitcoin.wallet.core.RealmManager
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class LoginManagerTest {

    val networkManager = mock(INetworkManager::class.java)
    val walletManager = mock(WalletManager::class.java)
    val realmManager = mock(RealmManager::class.java)
    val localStorage = mock(ILocalStorage::class.java)

    val loginManager = LoginManager(networkManager, walletManager, realmManager, localStorage)

    val words = listOf("one", "two")
    val identity = "identity"
    val pubKeys = hashMapOf<Int, String>(1 to "pub1", 2 to "pub2")
    val jwtToken = "jwtToken"
    val wallet = mock(WalletWrapper::class.java)
    val testObserver = TestObserver<Void>()

    @Before
    fun before() {
        whenever(walletManager.createWallet(words)).thenReturn(wallet)
        whenever(wallet.getIdentity()).thenReturn(identity)
        whenever(wallet.getPubKeys()).thenReturn(pubKeys)
    }

    @Test
    fun login_success() {
        whenever(networkManager.getJwtToken(identity, pubKeys)).thenReturn(Observable.just(jwtToken))
        whenever(realmManager.login(jwtToken)).thenReturn(Completable.complete())

        loginManager.login(words).subscribe(testObserver)

        verify(realmManager).login(jwtToken)
        verify(localStorage).saveWords(words)
        testObserver.assertComplete()
    }

    @Test
    fun login_failureRealmLogin() {
        val realmLoginException = Exception()

        whenever(networkManager.getJwtToken(identity, pubKeys)).thenReturn(Observable.just(jwtToken))
        whenever(realmManager.login(jwtToken)).thenReturn(Completable.error(realmLoginException))

        loginManager.login(words).subscribe(testObserver)

        verify(localStorage, never()).saveWords(words)
        testObserver.assertError(realmLoginException)
    }

    @Test
    fun login_failureNetwork() {
        val networkException = Exception()

        whenever(networkManager.getJwtToken(identity, pubKeys)).thenReturn(Observable.error(networkException))

        loginManager.login(words).subscribe(testObserver)

        verify(localStorage, never()).saveWords(words)
        testObserver.assertError(networkException)
    }
}