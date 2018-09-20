package bitcoin.wallet.modules.pin

import bitcoin.wallet.modules.pin.pinSubModules.EditPinAuthPresenter
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class EditPinAuthPresenterTest {

    private val interactor = Mockito.mock(PinModule.IInteractor::class.java)
    private val router = Mockito.mock(PinModule.IRouter::class.java)
    private val view = Mockito.mock(PinModule.IView::class.java)

    private val presenter = EditPinAuthPresenter(interactor, router)

    @Before
    fun setUp() {
        presenter.view = view
    }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(interactor)
        verifyNoMoreInteractions(router)
    }

    @Test
    fun viewDidLoad() {

        presenter.viewDidLoad()

        verify(view).setTitleForEditPinAuth()
        verify(view).setDescriptionForEditPinAuth()
    }

    @Test
    fun goToPinEdit() {
        presenter.goToPinEdit()

        verify(router).goToPinEdit()
    }

}
