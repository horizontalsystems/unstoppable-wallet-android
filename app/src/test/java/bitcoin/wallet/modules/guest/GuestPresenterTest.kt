package bitcoin.wallet.modules.guest

import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class GuestPresenterTest {

    private val interactor = mock(GuestModule.IInteractor::class.java)
    private val router = mock(GuestModule.IRouter::class.java)
    private val view = mock(GuestModule.IView::class.java)

    private val presenter = GuestPresenter(interactor, router)

    @Before
    fun before() {
        presenter.view = view
    }

    @Test
    fun createWallet() {
        presenter.createWalletDidClick()

        verify(interactor).createWallet()
    }

    @Test
    fun restoreWallet() {
        presenter.restoreWalletDidClick()

        verify(router).navigateToRestore()
    }

    @Test
    fun didCreateWallet() {
        presenter.didCreateWallet()

        verify(router).navigateToBackupRoutingToMain()
    }

    @Test
    fun didFailToCreateWallet() {
        presenter.didFailToCreateWallet(Exception())

        verify(view).showError()
    }

}