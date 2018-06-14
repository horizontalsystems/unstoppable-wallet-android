package bitcoin.wallet.modules.restore

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.IMnemonic
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class RestoreWalletModuleInteractorTest {

    private val mnemonic = mock(IMnemonic::class.java)
    private val localStorage = mock(ILocalStorage::class.java)
    private var delegate = mock(RestoreModule.IInteractorDelegate::class.java)

    private var interactor = RestoreInteractor(mnemonic, localStorage)

    @Before
    fun before() {
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

        interactor.restore(words)

        verify(localStorage).saveWords(words)

        verify(delegate).didRestore()
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun restoreWallet_failure() {
        val words = listOf("first", "second", "etc")

        whenever(mnemonic.validateWords(words)).thenReturn(false)

        interactor.restore(words)

        verify(delegate).didFailToRestore()
        verifyNoMoreInteractions(delegate)
    }
}