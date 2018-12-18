package io.horizontalsystems.bankwallet.modules.backup

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test
import org.mockito.Mockito.mock

class BackupPresenterTest {

    private val interactor = mock(BackupModule.IInteractor::class.java)
    private val router = mock(BackupModule.IRouter::class.java)
    private val view = mock(BackupModule.IView::class.java)
    private val toMain = BackupPresenter.DismissMode.SET_PIN
    private val selfDismiss = BackupPresenter.DismissMode.DISMISS_SELF

    private val presenter = BackupPresenter(interactor, router, toMain)

    @Test
    fun onLaterClick() {
        presenter.onLaterClick()
        verify(router).navigateToSetPin()
    }

    @Test
    fun showWordsDidClick() {
        // presenter.showWordsDidClick()
        // verify(interactor).fetchWords()
    }

    @Test
    fun hideWordsDidClick() {
        presenter.view = view
        presenter.hideWordsDidClick()
        verify(view).hideWords()
    }

    @Test
    fun showConfirmationDidClick() {
        presenter.showConfirmationDidClick()
        verify(interactor).fetchConfirmationIndexes()
    }

    @Test
    fun hideConfirmationDidClick() {
        presenter.view = view
        presenter.hideConfirmationDidClick()
        verify(view).hideConfirmation()
    }

    @Test
    fun validateDidClick() {
        presenter.validateDidClick(hashMapOf())
        verify(interactor).validate(any())
    }

    @Test
    fun didFetchWords() {
        val words = listOf("tree", "2", "lemon")
        presenter.view = view

        presenter.didFetchWords(words)
        verify(view).showWords(words)
    }

    @Test
    fun didFetchConfirmationIndexes() {
        val indexes = listOf(1, 3)
        presenter.view = view

        presenter.didFetchConfirmationIndexes(indexes)
        verify(view).showConfirmationWithIndexes(indexes)
    }

    @Test
    fun didValidateSuccess() {
        presenter.didValidateSuccess()
        verify(router).navigateToSetPin()
    }

    @Test
    fun didValidateSuccess_withSelfDismiss() {
        val presenterWithSelfDismiss = BackupPresenter(interactor, router, selfDismiss)
        presenterWithSelfDismiss.didValidateSuccess()
        verify(router).close()
    }

    @Test
    fun didValidateFailure() {
        presenter.view = view

        presenter.didValidateFailure()
        verify(view).showConfirmationError()
    }
}
