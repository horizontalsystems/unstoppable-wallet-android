package bitcoin.wallet.modules.pin.unlock

import bitcoin.wallet.core.*
import com.nhaarman.mockito_kotlin.*
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Captor
import org.mockito.Mockito

class UnlockPinInteractorTest {

    private val delegate = Mockito.mock(UnlockPinModule.IUnlockPinInteractorDelegate::class.java)
    private val pinManager = Mockito.mock(IPinManager::class.java)
    private val localStorage = Mockito.mock(ILocalStorage::class.java)
    private val lockManager = Mockito.mock(ILockManager::class.java)
    private val wordsManager = Mockito.mock(IWordsManager::class.java)
    private val adapterManager = Mockito.mock(IAdapterManager::class.java)
    private val keystoreSafeExecute = Mockito.mock(IKeyStoreSafeExecute::class.java)
    val adapter = Mockito.mock(IAdapter::class.java)
    private var interactor = UnlockPinInteractor(keystoreSafeExecute, localStorage, wordsManager, adapterManager, pinManager, lockManager)

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
    fun cacheSecuredData_pinSafeLoad() {

        whenever(pinManager.pin).thenReturn(null)
        whenever(pinManager.isPinSet).thenReturn(true)

        interactor.cacheSecuredData()
        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue

        actionRunnable.run()

        verify(pinManager).safeLoad()
    }

    @Test
    fun cacheSecuredData_pinSafeLoad_notNeeded() {

        whenever(pinManager.pin).thenReturn("000000")
        whenever(pinManager.isPinSet).thenReturn(true)

        interactor.cacheSecuredData()
        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue

        actionRunnable.run()

        verify(pinManager, never()).safeLoad()
    }

    @Test
    fun cacheSecuredData_wordsManagerSafeLoad_NotNeeded() {
        val adapters = mutableListOf<IAdapter>(adapter)
        whenever(adapterManager.adapters).thenReturn(adapters)

        interactor.cacheSecuredData()
        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue

        actionRunnable.run()

        verify(wordsManager, never()).safeLoad()
        verify(adapterManager, never()).start()
    }

    @Test
    fun cacheSecuredData_wordsManagerSafeLoad() {
        whenever(adapterManager.adapters).thenReturn(mutableListOf())

        interactor.cacheSecuredData()
        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue

        actionRunnable.run()

        verify(wordsManager).safeLoad()
        verify(adapterManager).start()
    }

    @Test
    fun biometricUnlock() {
        whenever(localStorage.isBiometricOn).thenReturn(true)

        interactor.biometricUnlock()
        verify(delegate).showFingerprintInput()
    }

    @Test
    fun biometricUnlock_disabled() {
        whenever(localStorage.isBiometricOn).thenReturn(false)

        interactor.biometricUnlock()
        verify(delegate, never()).showFingerprintInput()
    }

    @Test
    fun unlock_success() {
        val pin = "0000"

        whenever(pinManager.validate(pin)).thenReturn(true)

        val valid = interactor.unlock(pin)

        Assert.assertTrue(valid)
        verify(lockManager).onUnlock()
    }

    @Test
    fun unlock_failure() {
        val pin = "0000"

        whenever(pinManager.validate(pin)).thenReturn(false)

        val valid = interactor.unlock(pin)
        Assert.assertFalse(valid)
        verify(lockManager, never()).onUnlock()
    }

    @Test
    fun onUnlock() {
        interactor.onUnlock()

        verify(delegate).unlock()
        verify(lockManager).onUnlock()
    }
}
