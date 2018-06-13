package bitcoin.wallet.modules.restoreWallet

import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class RestoreWalletModulePresenterTest {

    private val presenter = RestoreWalletModulePresenter()
    private val interactor = mock(RestoreWalletModule.IInteractor::class.java)
    private val router = mock(RestoreWalletModule.IRouter::class.java)
    private val view = mock(RestoreWalletModule.IView::class.java)

    @Before
    fun before() {
        presenter.interactor = interactor
        presenter.router = router
        presenter.view = view
    }

    @Test
    fun onRestoreButtonClick() {
        val words = listOf("yahoo", "google", "facebook")

        presenter.onRestoreButtonClick(words)

        verify(interactor).restoreWallet(words)
    }

    @Test
    fun didRestoreWallet() {
        presenter.didRestoreWallet()

        verify(router).navigateToMainScreen()
    }

    @Test
    fun failureRestoreWallet() {
        val exception = RestoreWalletModule.InvalidWordsException()

        presenter.failureRestoreWallet(exception)

        verify(view).showInvalidWordsError()
    }

}