//package io.horizontalsystems.bankwallet.modules.pin.unlock
//
//import com.nhaarman.mockito_kotlin.*
//import io.horizontalsystems.bankwallet.core.*
//import io.horizontalsystems.bankwallet.core.managers.AuthManager
//import io.horizontalsystems.bankwallet.core.managers.OneTimeTimer
//import io.horizontalsystems.bankwallet.entities.LockoutState
//import org.junit.After
//import org.junit.Assert
//import org.junit.Before
//import org.junit.Test
//import org.mockito.Captor
//import org.mockito.Mockito
//import java.util.*
//
//class UnlockPinInteractorTest {
//
//    private val delegate = Mockito.mock(UnlockPinModule.IUnlockPinInteractorDelegate::class.java)
//    private val pinManager = Mockito.mock(IPinManager::class.java)
//    private val localStorage = Mockito.mock(ILocalStorage::class.java)
//    private val lockManager = Mockito.mock(ILockManager::class.java)
//    private val authManager = Mockito.mock(AuthManager::class.java)
//    private val encryptionManager = Mockito.mock(IEncryptionManager::class.java)
//    private val keystoreSafeExecute = Mockito.mock(IKeyStoreSafeExecute::class.java)
//    private val lockoutManager = Mockito.mock(ILockoutManager::class.java)
//    private val timer = Mockito.mock(OneTimeTimer::class.java)
//    private var interactor = UnlockPinInteractor(keystoreSafeExecute, localStorage, authManager, pinManager, lockManager, encryptionManager, lockoutManager, timer)
//
//    @Captor
//    private val actionRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()
//
//    @Captor
//    private val successRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()
//
//    @Captor
//    private val failureRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()
//
//    @Before
//    fun setUp() {
//        interactor.delegate = delegate
//        val state = LockoutState.Unlocked(3)
//        whenever(lockoutManager.currentState).thenReturn(state)
//    }
//
//    @After
//    fun tearDown() {
////        verifyNoMoreInteractions(delegate)
//    }
//
//    @Test
//    fun cacheSecuredData_pinSafeLoad() {
//
//        whenever(pinManager.pin).thenReturn(null)
//        whenever(pinManager.isPinSet).thenReturn(true)
//
//        interactor.cacheSecuredData()
//        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())
//
//        val actionRunnable = actionRunnableCaptor.firstValue
//
//        actionRunnable.run()
//
//        verify(pinManager).safeLoad()
//    }
//
//    @Test
//    fun cacheSecuredData_pinSafeLoad_notNeeded() {
//
//        whenever(pinManager.pin).thenReturn("000000")
//        whenever(pinManager.isPinSet).thenReturn(true)
//
//        interactor.cacheSecuredData()
//        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())
//
//        val actionRunnable = actionRunnableCaptor.firstValue
//
//        actionRunnable.run()
//
//        verify(pinManager, never()).safeLoad()
//    }
//
//    @Test
//    fun biometricUnlock_enabled() {
//        whenever(localStorage.isBiometricOn).thenReturn(true)
//
//        val result = interactor.isBiometricOn()
//        Assert.assertEquals(true, result)
//    }
//
//    @Test
//    fun biometricUnlock_disabled() {
//        whenever(localStorage.isBiometricOn).thenReturn(false)
//
//        val result = interactor.isBiometricOn()
//        Assert.assertEquals(false, result)
//    }
//
//    @Test
//    fun unlock_success() {
//        val pin = "0000"
//
//        whenever(pinManager.validate(pin)).thenReturn(true)
//
//        val valid = interactor.unlock(pin)
//
//        Assert.assertTrue(valid)
//        verify(lockManager).onUnlock()
//    }
//
//    @Test
//    fun unlock_failure() {
//        val pin = "0000"
//
//        whenever(pinManager.validate(pin)).thenReturn(false)
//
//        val valid = interactor.unlock(pin)
//        Assert.assertFalse(valid)
//        verify(lockManager, never()).onUnlock()
//    }
//
//    @Test
//    fun onUnlock() {
//        interactor.onUnlock()
//
//        verify(delegate).unlock()
//        verify(lockManager).onUnlock()
//    }
//
//
//    @Test
//    fun updateFailedAttempt() {
//        val pin = "0000"
//
//        whenever(pinManager.validate(pin)).thenReturn(false)
//
//        interactor.unlock(pin)
//        verify(lockoutManager).didFailUnlock()
//    }
//
//    @Test
//    fun updateLockoutState_FailedAttempt() {
//        val pin = "0000"
//
//        val state = LockoutState.Unlocked(3)
//
//        whenever(pinManager.validate(pin)).thenReturn(false)
//        whenever(lockoutManager.currentState).thenReturn(state)
//
//        interactor.unlock(pin)
//        verify(delegate).updateLockoutState(state)
//    }
//
//    @Test
//    fun startLockoutTimerOnWrongPin() {
//        val date = Date()
//        val pin = "0000"
//
//        val state = LockoutState.Locked(date)
//
//        whenever(pinManager.validate(pin)).thenReturn(false)
//        whenever(lockoutManager.currentState).thenReturn(state)
//
//        interactor.unlock(pin)
//
//        verify(timer).schedule(state.until)
//    }
//
//    @Test
//    fun updateLockoutState() {
//        val date = Date()
//        val state = LockoutState.Locked(date)
//
//        whenever(lockoutManager.currentState).thenReturn(state)
//
//        interactor.updateLockoutState()
//
//        verify(delegate).updateLockoutState(state)
//    }
//
//    @Test
//    fun startLockoutTimerOnLockedState() {
//        val date = Date()
//
//        val state = LockoutState.Locked(date)
//
//        whenever(lockoutManager.currentState).thenReturn(state)
//
//        interactor.updateLockoutState()
//
//        verify(timer).schedule(state.until)
//    }
//
//    @Test
//    fun updateStateOnLockoutTimerFire() {
//        val state = LockoutState.Unlocked(1)
//
//        whenever(lockoutManager.currentState).thenReturn(state)
//
//        interactor.onFire()
//
//        verify(delegate).updateLockoutState(state)
//    }
//
//    @Test
//    fun dropFailedAttempts() {
//        val pin = "0000"
//        whenever(pinManager.validate(pin)).thenReturn(true)
//
//        interactor.unlock(pin)
//
//        verify(lockoutManager).dropFailedAttempts()
//    }
//
//}
