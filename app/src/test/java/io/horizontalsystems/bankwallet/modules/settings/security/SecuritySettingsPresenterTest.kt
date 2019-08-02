package io.horizontalsystems.bankwallet.modules.settings.security

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test

class SecuritySettingsPresenterTest {

    private lateinit var interactor: SecuritySettingsModule.ISecuritySettingsInteractor
    private val router = mock<SecuritySettingsModule.ISecuritySettingsRouter>()
    private val view = mock<SecuritySettingsModule.ISecuritySettingsView>()
    private lateinit var presenter: SecuritySettingsPresenter

    @Before
    fun setUp() {
        interactor = mock {
        }

        presenter = SecuritySettingsPresenter(router, interactor)
        presenter.view = view
    }


    @Test
    fun testSetBiometricUnlockOnOnLoad() {
        presenter.viewDidLoad()

        verify(view).setBackedUp(any())
    }

    @Test
    fun testSetBiometricUnlockOffOnLoad() {
        interactor = mock { on { isPinEnabled } doReturn false }

        presenter = SecuritySettingsPresenter(router, interactor)
        presenter.view = view
        presenter.viewDidLoad()

        verify(view).setPinEnabled(false)
    }

    @Test
    fun didTapEditPin() {
        presenter.didTapEditPin()
        verify(router).showEditPin()
    }

    @Test
    fun didBackup() {
        presenter.didBackup(0)
        verify(view).setBackedUp(true)
    }

    @Test
    fun onClear() {
        presenter.onClear()

        verify(interactor).clear()
    }

}
