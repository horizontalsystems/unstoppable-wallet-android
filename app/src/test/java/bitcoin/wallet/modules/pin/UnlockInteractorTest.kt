package bitcoin.wallet.modules.pin

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.ISettingsManager
import bitcoin.wallet.modules.pin.pinSubModules.UnlockInteractor
import com.nhaarman.mockito_kotlin.atMost
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class UnlockInteractorTest {

    private val delegate = Mockito.mock(PinModule.IInteractorDelegate::class.java)
    private val storage = Mockito.mock(ILocalStorage::class.java)
    private val settings = Mockito.mock(ISettingsManager::class.java)

    private var interactor = UnlockInteractor(storage, settings)

    @Before
    fun setUp() {
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
    fun submit_success() {

        val pin = "123456"

        whenever(storage.getPin()).thenReturn(pin)

        interactor.submit(pin)

        verify(delegate).onCorrectPinSubmitted()
    }

    @Test
    fun submit_fail() {

        val pin = "111111"
        val pin2 = "123456"

        whenever(storage.getPin()).thenReturn(pin)

        interactor.submit(pin2)

        verify(delegate).onWrongPinSubmitted()
    }

    @Test
    fun onBackPressed() {
        interactor.onBackPressed()

        verify(delegate).onMinimizeApp()
    }

}
