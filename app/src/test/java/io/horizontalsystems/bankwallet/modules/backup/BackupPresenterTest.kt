package io.horizontalsystems.bankwallet.modules.backup

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class BackupPresenterTest {

    private val interactor = mock(BackupModule.IInteractor::class.java)
    private val router = mock(BackupModule.IRouter::class.java)
    private val view = mock(BackupModule.IView::class.java)
    private val state = mock(BackupModule.BackupModuleState::class.java)

    private val toMain = BackupPresenter.DismissMode.SET_PIN
    private val selfDismiss = BackupPresenter.DismissMode.DISMISS_SELF

    private lateinit var presenter: BackupPresenter


    @Before
    fun before() {
        presenter = BackupPresenter(interactor, router, toMain, state)
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {
        val page = 0
        presenter.viewDidLoad()
        verify(view).loadPage(page)
    }

    @Test
    fun onNextClick() {
        whenever(state.canLoadNextPage()).thenReturn(true)
        presenter.onNextClick()
        verify(state).canLoadNextPage()
        verify(view).loadPage(any())
    }

    @Test
    fun onNextClick_lastPage() {
        whenever(state.canLoadNextPage()).thenReturn(false)
        presenter.onNextClick()
        verify(state).canLoadNextPage()
        verify(view).validateWords()
    }

    @Test
    fun onBackClick() {
        whenever(state.canLoadPrevPage()).thenReturn(true)

        presenter.onBackClick()
        verify(state).canLoadPrevPage()
        verify(view).loadPage(any())
    }

    @Test
    fun onBackClick_firstPage() {
        val presenterWithSelfDismiss = BackupPresenter(interactor, router, selfDismiss, state)
        whenever(state.canLoadPrevPage()).thenReturn(false)
        whenever(interactor.shouldShowTermsConfirmation()).thenReturn(false)

        presenterWithSelfDismiss.onBackClick()
        verify(state).canLoadPrevPage()
        verify(router).close()
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
        verify(view).showConfirmationWords(indexes)
    }

    @Test
    fun didValidateSuccess() {
        presenter.didValidateSuccess()
        verify(router).navigateToSetPin()
    }

    @Test
    fun didValidateSuccess_withSelfDismiss() {
        val presenterWithSelfDismiss = BackupPresenter(interactor, router, selfDismiss, state)
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
