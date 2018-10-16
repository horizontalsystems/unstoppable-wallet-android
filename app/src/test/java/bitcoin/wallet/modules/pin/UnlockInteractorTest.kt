package bitcoin.wallet.modules.pin

import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.ISettingsManager
import bitcoin.wallet.modules.RxBaseTest
import bitcoin.wallet.modules.pin.pinSubModules.UnlockInteractor
import bitcoin.wallet.viewHelpers.DateHelper
import com.nhaarman.mockito_kotlin.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Captor
import org.mockito.Mockito
import java.util.*

class UnlockInteractorTest {

    private val delegate = Mockito.mock(PinModule.IInteractorDelegate::class.java)
    private val storage = Mockito.mock(ILocalStorage::class.java)
    private val settings = Mockito.mock(ISettingsManager::class.java)
    private val keystoreSafeExecute = Mockito.mock(IKeyStoreSafeExecute::class.java)
    private var interactor = UnlockInteractor(storage, settings, keystoreSafeExecute)

    @Captor
    private val actionRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()

    @Captor
    private val successRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()

    @Captor
    private val failureRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()

    @Before
    fun setUp() {
        RxBaseTest.setup()
        interactor.delegate = delegate
    }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun viewDidLoad_fingerprintEnabled() {

        whenever(settings.isFingerprintEnabled()).thenReturn(true)
        interactor.viewDidLoad()

        verify(delegate).onFingerprintEnabled()
    }

    @Test
    fun viewDidLoad_fingerprintDisabled() {

        whenever(settings.isFingerprintEnabled()).thenReturn(false)
        interactor.viewDidLoad()

        verify(delegate, atMost(0)).onFingerprintEnabled()
    }

    @Test
    fun viewDidLoad_showBlockedScreen() {
        val fiveMinutesLater = DateHelper.minutesAfterNow(1)

        whenever(settings.getBlockTillDate()).thenReturn(fiveMinutesLater)
        interactor.viewDidLoad()

        verify(delegate).blockScreen()

        verify(delegate).unblockScreen()
    }

    @Test
    fun viewDidLoad_showBlockedScreen_timeout() {
        val now = Date().time

        whenever(settings.getBlockTillDate()).thenReturn(now)
        interactor.viewDidLoad()

        verify(delegate, atMost(0)).blockScreen()
    }

    @Test
    fun viewDidLoad_notBlocked() {
        whenever(settings.getBlockTillDate()).thenReturn(null)
        interactor.viewDidLoad()

        verify(delegate, atMost(0)).blockScreen()
    }

    @Test
    fun submit_success() {
        val defaultAttemptsLeft = 5
        val pin = "123456"

        whenever(storage.getPin()).thenReturn(pin)

        interactor.submit(pin)

        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue

        actionRunnable.run()

        verify(delegate).onCorrectPinSubmitted()
        verify(settings).setUnlockAttemptsLeft(defaultAttemptsLeft)
    }

    @Test
    fun onBackPressed() {
        interactor.onBackPressed()

        verify(delegate).onMinimizeApp()
    }

    @Test
    fun onWrongPinSubmit_showFiveAttemptsLeft() {
        val pin = "111111"
        val pin2 = "123456"
        val attemptsLeft = 5

        whenever(storage.getPin()).thenReturn(pin)
        whenever(settings.getUnlockAttemptsLeft()).thenReturn(attemptsLeft)

        interactor.submit(pin2)

        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue

        actionRunnable.run()

        verify(delegate).showAttemptsLeftWarning(attemptsLeft)
        verify(settings).setUnlockAttemptsLeft(attemptsLeft-1)
    }

    @Test
    fun onWrongPinSubmit_blockScreen() {
        val pin = "111111"
        val pin2 = "123456"
        val attemptsLeft = 0
        val defaultAttemptsLeft = 5

        whenever(storage.getPin()).thenReturn(pin)
        whenever(settings.getUnlockAttemptsLeft()).thenReturn(attemptsLeft)

        interactor.submit(pin2)

        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue

        actionRunnable.run()

        verify(delegate).blockScreen()
        verify(settings).setUnlockAttemptsLeft(defaultAttemptsLeft)
    }

}
