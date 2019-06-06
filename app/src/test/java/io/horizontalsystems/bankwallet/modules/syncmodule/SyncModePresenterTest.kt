package io.horizontalsystems.bankwallet.modules.syncmodule

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
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
    fun viewDidLoad(){
        val syncMode = SyncMode.FAST

        whenever(interactor.getSyncMode()).thenReturn(syncMode)

        presenter.viewDidLoad()
        verify(interactor).getSyncMode()
        verify(view).updateSyncMode(syncMode)
    }

}
