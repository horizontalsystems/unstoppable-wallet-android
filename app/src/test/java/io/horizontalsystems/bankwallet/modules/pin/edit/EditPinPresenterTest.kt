package io.horizontalsystems.bankwallet.modules.pin.edit

import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class EditPinPresenterTest {

    private val interactor = Mockito.mock(PinModule.IPinInteractor::class.java)
    private val router = Mockito.mock(EditPinModule.IEditPinRouter::class.java)
    private val view = Mockito.mock(PinModule.IPinView::class.java)
    private var presenter = EditPinPresenter(interactor, router)

    @Before
    fun setUp() {
        RxBaseTest.setup()
        presenter.view = view
    }


    @Test
    fun viewDidLoad_setTitle() {
        presenter.viewDidLoad()
        verify(view).setTitle(any())
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()
        verify(view).addPages(any())
        verify(view).showCancel()
    }

    @Test
    fun onCancel() {
        presenter.onCancel()
        verify(router).dismiss()
    }

    @Test
    fun didSavePin() {
        presenter.didSavePin()
        verify(view).showSuccess()
        verify(router).dismiss()
    }

    @Test
    fun onBackPressed() {
        presenter.onBackPressed()
        verify(router).dismiss()
    }
}
