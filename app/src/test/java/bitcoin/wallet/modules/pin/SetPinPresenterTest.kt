package bitcoin.wallet.modules.pin

import bitcoin.wallet.modules.pin.pinSubModules.SetPinPresenter
import com.nhaarman.mockito_kotlin.atLeast
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class SetPinPresenterTest {

    private val interactor = Mockito.mock(PinModule.IInteractor::class.java)
    private val router = Mockito.mock(PinModule.IRouter::class.java)
    private val view = Mockito.mock(PinModule.IView::class.java)

    private val presenter = SetPinPresenter(interactor, router)

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

        verify(view).setTitleEnterPin()
        verify(view).setDescriptionEnterPin()
    }

    @Test
    fun onOneDigitEnter() {

        presenter.onEnterDigit(3)

        verify(view).highlightPinMask(1)
    }

    @Test
    fun onTwoDigitsEnter() {

        presenter.onEnterDigit(3)
        presenter.onEnterDigit(4)

        verify(view, atLeast(1)).highlightPinMask(2)
    }

    @Test
    fun onClickDelete() {

        presenter.onEnterDigit(3)
        presenter.onEnterDigit(4)
        reset(view)

        presenter.onClickDelete()

        verify(view).highlightPinMask(1)
    }


    @Test
    fun onClickDone() {

        presenter.onEnterDigit(3)
        presenter.onEnterDigit(4)

        presenter.onClickDone()

        verify(interactor).submit("34")
    }


    @Test
    fun onErrorShortPinLength() {

        presenter.onErrorShortPinLength()

        verify(view).showErrorShortPinLength()
    }

    @Test
    fun goToPinConfirmation() {

        val pin = "123456"
        presenter.goToPinConfirmation(pin)

        verify(router).goToPinConfirmation(pin)
    }

}
