package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.modules.pin.PinModule
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class SetPinConfirmationPresenterTest {

    private val interactor = Mockito.mock(PinModule.IInteractor::class.java)
    private val router = Mockito.mock(PinModule.IRouter::class.java)
    private val view = Mockito.mock(PinModule.IView::class.java)

    private val presenter = SetPinConfirmationPresenter(interactor, router)

    @Before
    fun setUp() {
        presenter.view = view
    }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(view)
        verifyNoMoreInteractions(interactor)
        verifyNoMoreInteractions(router)
    }

    @Test
    fun viewDidLoad() {

        presenter.viewDidLoad()

        verify(view).setTitleEnterAgain()
        verify(view).setDescriptionEnterAgain()
    }

    @Test
    fun onSuccessPinSet() {

        presenter.onDidPinSet()

        verify(view).showSuccessPinSet()

    }

    @Test
    fun onErrorPinsDontMatch() {

        presenter.onErrorPinsDontMatch()

        verify(view).showErrorPinsDontMatch()
    }

    @Test
    fun onErrorFailedToSavePin() {

        presenter.onErrorFailedToSavePin()

        verify(view).showErrorFailedToSavePin()
    }

}
