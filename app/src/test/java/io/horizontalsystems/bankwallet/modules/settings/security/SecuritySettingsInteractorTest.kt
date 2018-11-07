package io.horizontalsystems.bankwallet.modules.settings.security

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ISystemInfoManager
import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.bankwallet.entities.BiometryType
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.*
import io.reactivex.subjects.PublishSubject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class SecuritySettingsInteractorTest {

    private val delegate = mock(SecuritySettingsModule.ISecuritySettingsInteractorDelegate::class.java)

    private lateinit var adapterManager: IAdapterManager
    private lateinit var localStorage: ILocalStorage
    private lateinit var wordsManager: IWordsManager
    private lateinit var sysInfoManager: ISystemInfoManager

    private lateinit var interactor: SecuritySettingsInteractor

    private val backedUpSubject = PublishSubject.create<Boolean>()

    @Before
    fun setUp() {
        RxBaseTest.setup()

        adapterManager = mock {
        }

        wordsManager = mock {
            on { isBackedUp } doReturn true
            on { backedUpSubject } doReturn backedUpSubject
        }

        localStorage = mock {
            on { isBiometricOn } doReturn true
        }

        sysInfoManager = mock {
            on { biometryType } doReturn BiometryType.FINGER
        }

        interactor = SecuritySettingsInteractor(adapterManager, wordsManager, localStorage, sysInfoManager)

        interactor.delegate = delegate
    }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun getBiometryType() {
        Assert.assertEquals(interactor.biometryType, BiometryType.FINGER)
    }

    @Test
    fun isBackedUp() {
        Assert.assertTrue(interactor.isBackedUp)
    }

    @Test
    fun isNotBackedUp() {
        wordsManager = mock {
            on { isBackedUp } doReturn false
            on { backedUpSubject } doReturn backedUpSubject
        }
        interactor = SecuritySettingsInteractor(adapterManager, wordsManager, localStorage, sysInfoManager)
        interactor.delegate = delegate

        Assert.assertFalse(interactor.isBackedUp)
    }

    @Test
    fun getBiometricUnlockOn() {
        Assert.assertTrue(interactor.getBiometricUnlockOn())
    }

    @Test
    fun getBiometricUnlockOff() {
        localStorage = mock {
            on { isBiometricOn } doReturn false
        }
        interactor = SecuritySettingsInteractor(adapterManager, wordsManager, localStorage, sysInfoManager)
        interactor.delegate = delegate

        Assert.assertFalse(interactor.getBiometricUnlockOn())
    }

    @Test
    fun setBiometricUnlockOn() {
        interactor.setBiometricUnlockOn(false)
        verify(localStorage).isBiometricOn = false
    }

    @Test
    fun unlinkWallet() {
        interactor.unlinkWallet()
        verify(adapterManager).clear()
        verify(localStorage).clearAll()
        verify(delegate).didUnlinkWallet()
    }

    @Test
    fun testBackedUpSubjectTrue() {
        backedUpSubject.onNext(true)
        verify(delegate).didBackup()
    }

    @Test
    fun testBackedUpSubjectFalse() {
        backedUpSubject.onNext(false)
        verify(delegate, never()).didBackup()
    }
}
