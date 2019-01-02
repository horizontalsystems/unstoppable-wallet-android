package io.horizontalsystems.bankwallet.modules.settings.security

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.BiometryType
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.subjects.PublishSubject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class SecuritySettingsInteractorTest {

    private val delegate = mock(SecuritySettingsModule.ISecuritySettingsInteractorDelegate::class.java)
    private val walletManager = mock(IWalletManager::class.java)
    private val transactionRepository = mock(ITransactionRecordStorage::class.java)
    private val exchangeRateRepository = mock(IRateStorage::class.java)

    private lateinit var interactor: SecuritySettingsInteractor
    private lateinit var localStorage: ILocalStorage
    private lateinit var wordsManager: IWordsManager
    private lateinit var systemInfoManager: ISystemInfoManager

    private val backedUpSubject = PublishSubject.create<Boolean>()

    @Before
    fun setup() {
        RxBaseTest.setup()

        wordsManager = mock {
            on { isBackedUp } doReturn true
            on { backedUpSubject } doReturn backedUpSubject
        }

        localStorage = mock {
            on { isBiometricOn } doReturn true
        }

        systemInfoManager = mock {
            on { biometryType } doReturn BiometryType.FINGER
        }

        interactor = SecuritySettingsInteractor(walletManager, wordsManager, localStorage, transactionRepository, exchangeRateRepository, systemInfoManager)
        interactor.delegate = delegate
    }

    @After
    fun teardown() {
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun getBiometryType() {
        assertEquals(BiometryType.FINGER, interactor.biometryType)
    }

    @Test
    fun isBackedUp() {
        assertTrue(interactor.isBackedUp)
    }

    @Test
    fun getBiometricUnlockOn() {
        assertTrue(interactor.getBiometricUnlockOn())
    }

    @Test
    fun isNotBackedUp() {
        wordsManager = mock {
            on { isBackedUp } doReturn false
            on { backedUpSubject } doReturn backedUpSubject
        }
        interactor = SecuritySettingsInteractor(walletManager, wordsManager, localStorage, transactionRepository, exchangeRateRepository, systemInfoManager)
        interactor.delegate = delegate

        assertFalse(interactor.isBackedUp)
    }

    @Test
    fun getBiometricUnlockOff() {
        localStorage = mock {
            on { isBiometricOn } doReturn false
        }
        interactor = SecuritySettingsInteractor(walletManager, wordsManager, localStorage, transactionRepository, exchangeRateRepository, systemInfoManager)
        interactor.delegate = delegate

        assertFalse(interactor.getBiometricUnlockOn())
    }

    @Test
    fun setBiometricUnlockOn() {
        interactor.setBiometricUnlockOn(false)
        verify(localStorage).isBiometricOn = false
    }

    @Test
    fun unlinkWallet() {
        interactor.unlinkWallet()

        verify(wordsManager).logout()
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
