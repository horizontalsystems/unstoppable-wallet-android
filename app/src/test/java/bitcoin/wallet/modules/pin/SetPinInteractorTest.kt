package bitcoin.wallet.modules.pin

import bitcoin.wallet.modules.pin.pinSubModules.SetPinInteractor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class SetPinInteractorTest {

    private var interactor = SetPinInteractor()
    private val delegate = mock(PinModule.IInteractorDelegate::class.java)

    @Before
    fun setUp() {
        interactor.delegate = delegate
    }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun submit_success() {

        val pin = "123456"

        interactor.submit(pin)

        verify(delegate).goToPinConfirmation(pin)
    }

    @Test
    fun submit_fail() {

        val pin = "123"

        interactor.submit(pin)

        verify(delegate).onErrorShortPinLength()
    }

}
