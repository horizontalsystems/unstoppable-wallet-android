package io.horizontalsystems.pin

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.IPinManager
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class PinInteractorTest {

    private val delegate = Mockito.mock(PinModule.IInteractorDelegate::class.java)
    private val pinManager = Mockito.mock(IPinManager::class.java)
    private var interactor = PinInteractor(pinManager)


    @Before
    fun setup() {
        interactor.delegate = delegate
    }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun validate_success() {
        val pin = "0000"
        interactor.set(pin)
        Assert.assertTrue(interactor.validate(pin))
    }

    @Test
    fun validate_failure() {
        val pin = "0000"
        val pin2 = "1111"
        interactor.set(pin)
        Assert.assertFalse(interactor.validate(pin2))
    }

    @Test
    fun save_successSave() {
        val pin = "0000"
        interactor.save(pin)

        verify(pinManager).store(pin)
        verify(delegate).didSavePin()
    }

    @Test
    fun unlock_success() {
        val pin = "0000"
        whenever(pinManager.validate(pin)).thenReturn(true)
        val isValid = interactor.unlock(pin)
        Assert.assertTrue(isValid)
    }

    @Test
    fun unlock_failure() {
        val pin = "0000"

        whenever(pinManager.validate(pin)).thenReturn(false)

        interactor.unlock(pin)

        val isValid = interactor.unlock(pin)
        Assert.assertFalse(isValid)
    }
}
