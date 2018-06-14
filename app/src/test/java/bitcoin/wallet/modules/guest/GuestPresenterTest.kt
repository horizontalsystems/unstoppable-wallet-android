package bitcoin.wallet.modules.guest

import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class GuestPresenterTest {

    private val presenter = GuestPresenter()
    private val interactor = mock(GuestModule.IInteractor::class.java)
    private val router = mock(GuestModule.IRouter::class.java)

    @Before
    fun before() {
        presenter.interactor = interactor
        presenter.router = router
    }

    @Test
    fun createWallet() {
        presenter.createWallet()

        verify(interactor).createWallet()
    }

    @Test
    fun restoreWallet() {
        presenter.restoreWallet()

        verify(router).openRestoreWalletScreen()
    }

    @Test
    fun didCreateWallet() {
        presenter.didCreateWallet()

        verify(router).openBackupScreen()
    }

}