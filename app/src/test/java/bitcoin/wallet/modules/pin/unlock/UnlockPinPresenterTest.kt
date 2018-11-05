package bitcoin.wallet.modules.pin.unlock

import bitcoin.wallet.modules.RxBaseTest
import bitcoin.wallet.modules.pin.PinModule
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.reset

class UnlockPinPresenterTest {

    private val interactor = Mockito.mock(UnlockPinModule.IUnlockPinInteractor::class.java)
    private val router = Mockito.mock(UnlockPinModule.IUnlockPinRouter::class.java)
    private val view = Mockito.mock(PinModule.IPinView::class.java)
    private var presenter = UnlockPinPresenter(interactor, router)

    @Before
    fun setUp() {
        RxBaseTest.setup()
        presenter.view = view
    }

    @Test
    fun testAddPages() {
        presenter.viewDidLoad()
        verify(view).addPages(any())
    }

    @Test
    fun onUnlockEnter() {
        val pin ="000000"
        presenter.onEnter(pin, 0)

        verify(view).fillCircles(pin.length, 0)
        verify(interactor).unlock(pin)
    }

    @Test
    fun onUnlockEnter_notEnough() {
        val pin ="00000"
        presenter.onEnter(pin, 0)

        verify(interactor, never()).unlock(pin)
    }

    @Test
    fun onCancel() {
        presenter.onCancel()
        verify(router).dismiss(false)
    }

    @Test
    fun onDelete() {
        val pin ="12345"
        presenter.onEnter(pin, 0)
        verify(view).fillCircles(5, 0)
        reset(view)
        presenter.onDelete(0)
        verify(view).fillCircles(4, 0)
    }

    @Test
    fun didBiometricUnlock() {
        presenter.didBiometricUnlock()
        verify(router).dismiss(true)
    }

    @Test
    fun unlock() {
        presenter.unlock()
        verify(router).dismiss(true)
    }

    @Test
    fun showFingerprintInput() {
        presenter.showFingerprintInput()
        verify(view).showFingerprintDialog()
    }

    @Test
    fun wrongPinSubmitted() {
        val pin ="12345"
        presenter.onEnter(pin, 0)
        verify(view).fillCircles(5, 0)
        reset(view)
        presenter.wrongPinSubmitted()
        verify(view).showPinWrong(0)
    }

    @Test
    fun onBiometricUnlock() {
        presenter.onBiometricUnlock()
        verify(interactor).onUnlock()
    }

    @Test
    fun showBiometricUnlock() {
        presenter.showBiometricUnlock()
        verify(interactor).biometricUnlock()
    }
}
