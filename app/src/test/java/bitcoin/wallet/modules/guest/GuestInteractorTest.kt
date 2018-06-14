package bitcoin.wallet.modules.guest

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.IMnemonic
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class GuestInteractorTest {

    private val mnemonic = mock(IMnemonic::class.java)
    private val localStorage = mock(ILocalStorage::class.java)
    private val delegate = mock(GuestModule.IInteractorDelegate::class.java)

    private val interactor = GuestInteractor(mnemonic, localStorage)

    @Before
    fun before() {
        interactor.delegate = delegate
    }

    @Test
    fun createWallet() {
        val words = listOf("1", "2", "etc")

        whenever(mnemonic.generateWords()).thenReturn(words)

        interactor.createWallet()

        verify(localStorage).saveWords(words)
        verify(delegate).didCreateWallet()
    }
}