//package io.horizontalsystems.bankwallet.modules.settings.security
//
//import com.nhaarman.mockito_kotlin.doReturn
//import com.nhaarman.mockito_kotlin.mock
//import com.nhaarman.mockito_kotlin.verify
//import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
//import io.horizontalsystems.bankwallet.core.*
//import io.horizontalsystems.bankwallet.entities.BiometryType
//import io.horizontalsystems.bankwallet.modules.RxBaseTest
//import io.reactivex.Flowable
//import org.junit.After
//import org.junit.Assert.*
//import org.junit.Before
//import org.junit.Test
//import org.mockito.Mockito.mock
//
//class SecuritySettingsInteractorTest {
//
//    private val delegate = mock(SecuritySettingsModule.ISecuritySettingsInteractorDelegate::class.java)
//
//    private lateinit var interactor: SecuritySettingsInteractor
//    private lateinit var localStorage: ILocalStorage
//    private lateinit var backupManager: IBackupManager
//    private lateinit var systemInfoManager: ISystemInfoManager
//    private lateinit var lockManager: ILockManager
//    private lateinit var pinManager: IPinManager
//
//    private val backedUpSignal = Flowable.empty<Int>()
//
//    @Before
//    fun setup() {
//        RxBaseTest.setup()
//
//        backupManager = mock {
//            on { nonBackedUpCountFlowable } doReturn backedUpSignal
//        }
//
//        localStorage = mock {
//            on { isBiometricOn } doReturn true
//        }
//
//        systemInfoManager = mock {
//            on { biometryType } doReturn BiometryType.FINGER
//        }
//
//        lockManager = mock {
//            on { isLocked } doReturn false
//        }
//
//        pinManager = mock {}
//
//        interactor = SecuritySettingsInteractor(backupManager, localStorage, systemInfoManager, pinManager)
//        interactor.delegate = delegate
//    }
//
//    @After
//    fun teardown() {
//        verifyNoMoreInteractions(delegate)
//    }
//
//    @Test
//    fun getBiometryType() {
//        assertEquals(BiometryType.FINGER, interactor.biometryType)
//    }
//
//    @Test
//    fun getBiometricUnlockOn() {
//        assertTrue(interactor.getBiometricUnlockOn())
//    }
//
//    @Test
//    fun getBiometricUnlockOff() {
//        localStorage = mock {
//            on { isBiometricOn } doReturn false
//        }
//        interactor = SecuritySettingsInteractor(backupManager, localStorage, systemInfoManager, pinManager)
//        interactor.delegate = delegate
//
//        assertFalse(interactor.getBiometricUnlockOn())
//    }
//
//    @Test
//    fun setBiometricUnlockOn() {
//        interactor.setBiometricUnlockOn(false)
//        verify(localStorage).isBiometricOn = false
//    }
//
//}
