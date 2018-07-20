package bitcoin.wallet.modules.guest

import android.security.keystore.UserNotAuthenticatedException
import bitcoin.wallet.blockchain.BlockchainManager
import bitcoin.wallet.core.IMnemonic
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class GuestInteractorTest {

    private val mnemonic = mock(IMnemonic::class.java)
    private val delegate = mock(GuestModule.IInteractorDelegate::class.java)
    private val blockchainManager = mock(BlockchainManager::class.java)

    private val interactor = GuestInteractor(mnemonic, blockchainManager)

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor.delegate = delegate
    }

    @Test
    fun createWallet_success() {
        val words = listOf("1", "2", "etc")

        whenever(mnemonic.generateWords()).thenReturn(words)

        interactor.createWallet()

        verify(blockchainManager).initNewWallet(words)

        verify(delegate).didCreateWallet()
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun createWallet_error() {
        val words = listOf("1", "2", "etc")
        val exception = UserNotAuthenticatedException()

        whenever(mnemonic.generateWords()).thenReturn(words)
        whenever(blockchainManager.initNewWallet(words)).thenThrow(exception)

        interactor.createWallet()

        verify(delegate).didFailToCreateWallet(exception)
        verifyNoMoreInteractions(delegate)
    }
}