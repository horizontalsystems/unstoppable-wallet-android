package io.horizontalsystems.bankwallet.modules.syncmodule

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.SyncMode
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class SyncModePresenterTest {

    private val interactor = Mockito.mock(SyncModeModule.IInteractor::class.java)
    private val router = Mockito.mock(SyncModeModule.IRouter::class.java)
    private val view = Mockito.mock(SyncModeModule.IView::class.java)
    private val state = SyncModeModule.State()

    private lateinit var presenter: SyncModePresenter

    @Before
    fun setUp() {
        presenter = SyncModePresenter(interactor, router, state)
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {
        val syncMode = SyncMode.FAST

        whenever(interactor.getSyncMode()).thenReturn(syncMode)

        presenter.viewDidLoad()
        verify(interactor).getSyncMode()
        verify(view).updateSyncMode(syncMode)
    }

    @Test
    fun onFastSyncModeSelect() {
        val syncMode = SyncMode.FAST
        presenter.onFastSyncModeSelect()
        verify(view).updateSyncMode(syncMode)
    }

    @Test
    fun onSlowSyncModeSelect() {
        val syncMode = SyncMode.SLOW
        presenter.onSlowSyncModeSelect()
        verify(view).updateSyncMode(syncMode)
    }

    @Test
    fun onNextClick() {
        presenter.onNextClick()
        verify(view).showConfirmationDialog()
    }

    @Test
    fun didConfirm() {
        val words = listOf("first", "second", "etc")
        state.syncMode = SyncMode.FAST
        presenter = SyncModePresenter(interactor, router, state)
        presenter.view = view
        val syncMode = state.syncMode!!

        presenter.didConfirm(words)
        verify(interactor).restore(words, syncMode)
    }

    @Test
    fun didRestore() {
        presenter.didRestore()
        verify(router).navigateToSetPin()
    }

    @Test
    fun didFailToRestore() {
        val exception = Exception()
        val errorTextRes = R.string.Restore_RestoreFailed
        presenter.didFailToRestore(exception)
        verify(view).showError(errorTextRes)
    }

}
