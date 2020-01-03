package io.horizontalsystems.bankwallet.modules.pin.unlock

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import io.horizontalsystems.bankwallet.entities.LockoutState
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import java.util.*

class UnlockPinPresenterTest {

    private val interactor = mock(UnlockPinModule.IInteractor::class.java)
    private val router = mock(UnlockPinModule.IRouter::class.java)
    private val view = mock(PinModule.IView::class.java)
    private lateinit var presenter: UnlockPinPresenter

    @Before
    fun setUp() {
        RxBaseTest.setup()

        presenter = UnlockPinPresenter(view, router, interactor, false)
    }

    @Test
    fun testAddPages() {
        presenter.viewDidLoad()
        verify(view).addPages(any())
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()
        verify(view).hideToolbar()
        verify(interactor).updateLockoutState()
    }

    @Test
    fun viewDidLoad_showCancelButton() {
        presenter = UnlockPinPresenter(view, router, interactor, true)
        presenter.viewDidLoad()
        verify(view).showBackButton()
        verify(interactor).updateLockoutState()
    }

    @Test
    fun onUnlockEnter() {
        val pin = "000000"
        presenter.onEnter(pin, 0)

        verify(view).fillCircles(pin.length, 0)
        verify(interactor).unlock(pin)
    }

    @Test
    fun onUnlockEnter_notEnough() {
        val pin = "00000"
        presenter.onEnter(pin, 0)

        verify(interactor, never()).unlock(pin)
    }

    @Test
    fun onUnlockEnter_wrongPageIndex() {
        val pin = "0000"
        presenter.onEnter(pin, -1)

        verify(view, never()).fillCircles(any(), any())
    }

    @Test
    fun onDelete() {
        val pin = "12345"
        presenter.onEnter(pin, 0)
        verify(view).fillCircles(5, 0)
        reset(view)
        presenter.onDelete(0)
        verify(view).fillCircles(4, 0)
    }


    @Test
    fun wrongPinSubmitted() {
        val pin = "12345"
        presenter.onEnter(pin, 0)
        verify(view).fillCircles(5, 0)
        reset(view)
        presenter.wrongPinSubmitted()
        verify(view).showPinWrong(0)
    }

    @Test
    fun onUnlock_onAppStart() {
        presenter = UnlockPinPresenter(view, router, interactor, false)

        presenter.unlock()
        verify(router).dismissModuleWithSuccess()
    }

    @Test
    fun onViewDidLoad_UpdateLockoutState() {
        presenter.viewDidLoad()

        verify(interactor).updateLockoutState()
    }

    @Test
    fun updateLockoutState_Unlocked() {
        val hasFailedAttempts = false
        val state = LockoutState.Unlocked(hasFailedAttempts)
        presenter.updateLockoutState(state)

        verify(view).showPinInput()
        verify(view, never()).showLockView(any())
    }

    @Test
    fun updateLockoutState_UnlockedWithFewAttempts() {
        val hasFailedAttempts = true
        val state = LockoutState.Unlocked(hasFailedAttempts)
        presenter.updateLockoutState(state)

        verify(view).showPinInput()
        verify(view, never()).showLockView(any())
    }

    @Test
    fun updateLockoutState_Locked() {
        val date = Date()
        val state = LockoutState.Locked(date)
        presenter.updateLockoutState(state)

        verify(view).showLockView(any())
        verify(view, never()).showPinInput()
    }

}
