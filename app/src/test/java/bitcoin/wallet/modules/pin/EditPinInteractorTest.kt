package bitcoin.wallet.modules.pin

import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.core.ISecuredStorage
import bitcoin.wallet.modules.pin.pinSubModules.EditPinInteractor
import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Captor
import org.mockito.Mockito
import org.mockito.Mockito.mock

class EditPinInteractorTest {

    private val delegate = mock(PinModule.IInteractorDelegate::class.java)
    private val iSecuredStorage = mock(ISecuredStorage::class.java)
    private val keystoreSafeExecute = Mockito.mock(IKeyStoreSafeExecute::class.java)
    private var interactor = EditPinInteractor(iSecuredStorage, keystoreSafeExecute)

    @Captor
    private val actionRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()

    @Captor
    private val successRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()

    @Captor
    private val failureRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()

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

        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue
        val successRunnable = successRunnableCaptor.firstValue

        actionRunnable.run()
        successRunnable.run()

        verify(iSecuredStorage).savePin(pin)
        verify(delegate).onDidPinSet()
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun submit_storageError() {

        val pin = "123456"

        interactor.submit(pin)

        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue
        val failureRunnable = failureRunnableCaptor.firstValue

        actionRunnable.run()
        failureRunnable.run()

        verify(delegate).onErrorFailedToSavePin()
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun submit_shortPinError() {

        val pin = "1234"

        interactor.submit(pin)

        verify(delegate).onErrorShortPinLength()
        verifyNoMoreInteractions(delegate)
    }

}
