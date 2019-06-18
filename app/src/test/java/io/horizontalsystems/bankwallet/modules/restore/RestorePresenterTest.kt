package io.horizontalsystems.bankwallet.modules.restore

import com.nhaarman.mockito_kotlin.verify
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.hdwalletkit.Mnemonic
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class RestorePresenterTest {

    private val interactor = Mockito.mock(RestoreModule.IInteractor::class.java)
    private val router = Mockito.mock(RestoreModule.IRouter::class.java)
    private val view = Mockito.mock(RestoreModule.IView::class.java)

    private lateinit var presenter: RestorePresenter

    @Before
    fun setUp() {
        presenter = RestorePresenter(interactor, router)
        presenter.view = view
    }

    @Test
    fun restoreDidClick() {
        val words = listOf("yahoo", "google", "facebook")
        presenter.restoreDidClick(words)
        verify(interactor).validate(words)
    }

    @Test
    fun didFailToValidate() {
        val exception = Mnemonic.MnemonicException("")
        val errorId = R.string.Restore_ValidationFailed
        presenter.didFailToValidate(exception)
        verify(view).showError(errorId)
    }

    @Test
    fun didValidate() {
        val words = listOf("yahoo", "google", "facebook")
        presenter.didValidate(words)
        verify(router).navigateToSetSyncMode(words)
    }

}
