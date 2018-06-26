package bitcoin.wallet.modules.guest

import bitcoin.wallet.core.IMnemonic
import bitcoin.wallet.core.managers.LoginManager
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class GuestInteractorTest {

    private val mnemonic = mock(IMnemonic::class.java)
    private val loginManager = mock(LoginManager::class.java)
    private val delegate = mock(GuestModule.IInteractorDelegate::class.java)

    private val interactor = GuestInteractor(mnemonic, loginManager)

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor.delegate = delegate
    }

    @Test
    fun createWallet_login() {
        val words = listOf("1", "2", "etc")

        whenever(mnemonic.generateWords()).thenReturn(words)
        whenever(loginManager.login(words)).thenReturn(Completable.complete())

        interactor.createWallet()

        verify(loginManager).login(words)

    }

    @Test
    fun createWallet_success() {
        val words = listOf("1", "2", "etc")

        whenever(mnemonic.generateWords()).thenReturn(words)
        whenever(loginManager.login(words)).thenReturn(Completable.complete())

        interactor.createWallet()

        verify(delegate).didCreateWallet()
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun createWallet_error() {
        val words = listOf("1", "2", "etc")

        whenever(mnemonic.generateWords()).thenReturn(words)
        whenever(loginManager.login(words)).thenReturn(Completable.error(Exception()))

        interactor.createWallet()

        verify(delegate).didFailToCreateWallet()
        verifyNoMoreInteractions(delegate)
    }
}