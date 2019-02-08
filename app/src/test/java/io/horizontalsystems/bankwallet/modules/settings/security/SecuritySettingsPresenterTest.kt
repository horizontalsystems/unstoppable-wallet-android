package io.horizontalsystems.bankwallet.modules.settings.security

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.horizontalsystems.bankwallet.entities.BiometryType
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class SecuritySettingsPresenterTest {

    private lateinit var interactor: SecuritySettingsModule.ISecuritySettingsInteractor
    private val router = mock<SecuritySettingsModule.ISecuritySettingsRouter>()
    private val view = mock<SecuritySettingsModule.ISecuritySettingsView>()
    private lateinit var presenter:SecuritySettingsPresenter

    @Before
    fun setUp() {
        interactor = mock {
            on { getBiometricUnlockOn() } doReturn true
            on { biometryType } doReturn BiometryType.FINGER
            on { isBackedUp } doReturn true
        }

        presenter = SecuritySettingsPresenter(router, interactor)
        presenter.view = view
    }


    @Test
    fun showTitle() {
        presenter.viewDidLoad()

        Mockito.verify(view).setTitle(any())
    }

    @Test
    fun testSetBiometricUnlockOnOnLoad() {
        presenter.viewDidLoad()

        verify(view).setBiometricUnlockOn(true)
    }

    @Test
    fun testSetBiometricUnlockOffOnLoad() {
        interactor = mock {
            on { getBiometricUnlockOn() } doReturn false
        }

        presenter = SecuritySettingsPresenter(router, interactor)
        presenter.view = view

        presenter.viewDidLoad()

        verify(view).setBiometricUnlockOn(false)
    }

    @Test
    fun testSetBiometricTypeFingerOnLoad() {
        presenter.viewDidLoad()

        verify(view).setBiometryType(BiometryType.FINGER)
    }

    @Test
    fun testSetBiometricTypeNoneOnLoad() {
        interactor = mock {
            on { biometryType } doReturn BiometryType.NONE
        }

        presenter = SecuritySettingsPresenter(router, interactor)
        presenter.view = view

        presenter.viewDidLoad()

        verify(view).setBiometryType(BiometryType.NONE)
    }

    @Test
    fun testSetBackedUpTrueOnLoad() {
        presenter.viewDidLoad()

        verify(view).setBackedUp(true)
    }

    @Test
    fun testSetBackedUpFalseOnLoad() {
        interactor = mock {
            on { isBackedUp } doReturn false
        }

        presenter = SecuritySettingsPresenter(router, interactor)
        presenter.view = view

        presenter.viewDidLoad()

        verify(view).setBackedUp(false)
    }

    @Test
    fun didSwitchBiometricUnlock() {
        presenter.didSwitchBiometricUnlock(true)
        verify(interactor).setBiometricUnlockOn(true)
    }

    @Test
    fun didTapEditPin() {
        presenter.didTapEditPin()
        verify(router).showEditPin()
    }

    @Test
    fun didTapBackupWallet() {
        presenter.didTapBackupWallet()
        verify(interactor).didTapOnBackupWallet()
    }

    @Test
    fun didTapRestoreWallet() {
        presenter.didTapRestoreWallet()
        verify(router).showRestoreWallet()
    }

    @Test
    fun confirmedUnlinkWallet() {
        presenter.confirmedUnlinkWallet()
        verify(interactor).unlinkWallet()
    }

    @Test
    fun didBackup() {
        presenter.didBackup()
        verify(view).setBackedUp(true)
    }

    @Test
    fun didUnlinkWallet() {
        presenter.didUnlinkWallet()
        verify(view).reloadApp()
    }

    @Test
    fun onClear() {
        presenter.onClear()

        verify(interactor).clear()
    }

}
