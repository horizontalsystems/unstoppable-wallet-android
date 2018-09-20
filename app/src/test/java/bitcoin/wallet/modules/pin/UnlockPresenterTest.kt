package bitcoin.wallet.modules.pin

import bitcoin.wallet.modules.pin.pinSubModules.UnlockPresenter
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class UnlockPresenterTest {

    private val interactor = Mockito.mock(PinModule.IInteractor::class.java)
    private val router = Mockito.mock(PinModule.IRouter::class.java)
    private val view = Mockito.mock(PinModule.IView::class.java)

    private val presenter = UnlockPresenter(interactor, router)


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

        verify(view).hideToolbar()
        verify(view).setDescriptionForUnlock()
        verify(interactor).viewDidLoad()
    }

    @Test
    fun onFingerprintEnabled() {

        presenter.onFingerprintEnabled()

        verify(view).showFingerprintDialog()
    }

    @Test
    fun onAllDigitsEntered() {

        var enteredPin = ""

        for (i in 1..PinModule.pinLength) {
            presenter.onEnterDigit(i)
            enteredPin += i
        }

        verify(interactor).submit(enteredPin)
    }

    @Test
    fun onCorrectPinSubmitted() {
        presenter.onCorrectPinSubmitted()

        verify(router).unlockWallet()
    }

    @Test
    fun onWrongPinSubmitted() {
        presenter.onWrongPinSubmitted()

        verify(view).clearPinMaskWithDelay()
        verify(view).showErrorWrongPin()
    }

    @Test
    fun onMinimizeApp() {
        presenter.onMinimizeApp()

        verify(view).minimizeApp()
    }

}
