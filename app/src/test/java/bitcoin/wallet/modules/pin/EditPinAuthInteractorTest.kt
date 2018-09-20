package bitcoin.wallet.modules.pin

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.modules.pin.pinSubModules.EditPinAuthInteractor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class EditPinAuthInteractorTest {

    private val delegate = mock(PinModule.IInteractorDelegate::class.java)
    private val storage = mock(ILocalStorage::class.java)
    private var interactor = EditPinAuthInteractor(storage)

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

        whenever(storage.getPin()).thenReturn(pin)

        interactor.submit(pin)

        verify(delegate).goToPinEdit()
    }

    @Test
    fun submit_fail() {
        val pin = "111111"
        val pin2 = "123456"

        whenever(storage.getPin()).thenReturn(pin)

        interactor.submit(pin2)

        verify(delegate).onWrongPinSubmitted()
    }

}
