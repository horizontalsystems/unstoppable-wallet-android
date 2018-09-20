package bitcoin.wallet.modules.pin

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.modules.pin.pinSubModules.EditPinInteractor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class EditPinInteractorTest {

    private val delegate = mock(PinModule.IInteractorDelegate::class.java)
    private val storage = mock(ILocalStorage::class.java)
    private var interactor = EditPinInteractor(storage)

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

        verify(storage).savePin(pin)
        verify(delegate).onDidPinSet()
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun submit_storageError() {

        val pin = "123456"

        whenever(storage.savePin(pin)).thenThrow(Exception::class.java)

        interactor.submit(pin)

        verify(delegate).onErrorFailedToSavePin()
        verifyNoMoreInteractions(delegate)
    }

}
