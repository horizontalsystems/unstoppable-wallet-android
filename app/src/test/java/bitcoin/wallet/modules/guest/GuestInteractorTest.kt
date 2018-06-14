package bitcoin.wallet.modules.guest

import bitcoin.wallet.lib.WalletDataManager
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class GuestInteractorTest {

    private val interactor = GuestInteractor()
    private val walletDataProvider = mock(WalletDataManager::class.java)
    private val delegate = mock(GuestModule.IInteractorDelegate::class.java)

    @Before
    fun before() {
        interactor.delegate = delegate
        interactor.walletDataProvider = walletDataProvider
    }

    @Test
    fun createWallet() {
        interactor.createWallet()

        verify(walletDataProvider).createWallet()
        verify(delegate).didCreateWallet()
    }
}