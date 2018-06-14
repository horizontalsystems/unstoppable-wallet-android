package bitcoin.wallet.modules.restore

import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class RestoreWalletModulePresenterTest {

    private val interactor = mock(RestoreModule.IInteractor::class.java)
    private val router = mock(RestoreModule.IRouter::class.java)
    private val view = mock(RestoreModule.IView::class.java)

    private val presenter = RestorePresenter(interactor, router)

    @Before
    fun before() {
        presenter.view = view
    }

    @Test
    fun restoreDidClick() {
        val words = listOf("yahoo", "google", "facebook")

        presenter.restoreDidClick(words)

        verify(interactor).restore(words)
    }

    @Test
    fun cancelDidClick() {
        presenter.cancelDidClick()

        verify(router).close()
    }

    @Test
    fun didRestoreWallet() {
        presenter.didRestore()

        verify(router).navigateToMain()
    }

    @Test
    fun didFailToRestore() {
        presenter.didFailToRestore()

        verify(view).showInvalidWordsError()
    }

}