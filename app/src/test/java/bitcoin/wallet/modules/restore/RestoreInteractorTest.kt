package bitcoin.wallet.modules.restore

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

class RestoreInteractorTest {

    private val mnemonic = mock(IMnemonic::class.java)
    private val loginManager = mock(LoginManager::class.java)
    private var delegate = mock(RestoreModule.IInteractorDelegate::class.java)

    private var interactor = RestoreInteractor(mnemonic, loginManager)

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor.delegate = delegate
    }

    @Test
    fun restoreWallet_validate() {
        val words = listOf("first", "second", "etc")

        interactor.restore(words)

        verify(mnemonic).validateWords(words)
    }

    @Test
    fun restoreWallet_success() {
        val words = listOf("first", "second", "etc")

        whenever(mnemonic.validateWords(words)).thenReturn(true)
        whenever(loginManager.login(words)).thenReturn(Completable.complete())

        interactor.restore(words)

        verify(delegate).didRestore()
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun restoreWallet_failureWordsError() {
        val words = listOf("first", "second", "etc")

        whenever(mnemonic.validateWords(words)).thenReturn(false)

        interactor.restore(words)

        verify(delegate).didFailToRestore()
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun restoreWallet_failureLoginError() {
        val words = listOf("first", "second", "etc")
        val exception = Exception()

        whenever(mnemonic.validateWords(words)).thenReturn(true)
        whenever(loginManager.login(words)).thenReturn(Completable.error(exception))

        interactor.restore(words)

        verify(delegate).didFailToRestore()
        verifyNoMoreInteractions(delegate)
    }
}