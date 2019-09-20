package io.horizontalsystems.bankwallet.modules.pin.edit

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class EditPinPresenterTest {

    private val interactor = Mockito.mock(PinModule.IInteractor::class.java)
    private val router = Mockito.mock(EditPinModule.IRouter::class.java)
    private val view = Mockito.mock(PinModule.IView::class.java)
    private var presenter = EditPinPresenter(view, router, interactor)

    @Before
    fun setUp() {
        RxBaseTest.setup()
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
        verify(view).showBackButton()
    }

    @Test
    fun didSavePin() {
        presenter.didSavePin()
        verify(router).dismissModuleWithSuccess()
    }

}
