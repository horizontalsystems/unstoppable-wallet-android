package io.horizontalsystems.bankwallet.modules.settings.main

import com.nhaarman.mockito_kotlin.any
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class MainSettingsPresenterTest {

    private val interactor = mock(MainSettingsModule.IMainSettingsInteractor::class.java)
    private val router = mock(MainSettingsModule.IMainSettingsRouter::class.java)
    private val view = mock(MainSettingsModule.IMainSettingsView::class.java)


    private val presenter = MainSettingsPresenter(router, interactor)

    @Before
    fun setUp() {
        presenter.view = view
    }

    @Test
    fun showTitle() {
        presenter.viewDidLoad()

        verify(view).setTitle(any())
    }

    @Test
    fun didTapSecurity() {
        presenter.didTapSecurity()
        verify(router).showSecuritySettings()
    }

    @Test
    fun didTapBaseCurrency() {
        presenter.didTapBaseCurrency()
        verify(router).showBaseCurrencySettings()
    }

    @Test
    fun didTapLanguage() {
        presenter.didTapLanguage()
        verify(router).showLanguageSettings()
    }

    @Test
    fun didSwitchLightModeOn() {
        val lightModeOn = true
        presenter.didSwitchLightMode(lightModeOn)
        verify(interactor).setLightMode(lightModeOn)
    }

    @Test
    fun didSwitchLightModeOff() {
        val lightModeOn = false
        presenter.didSwitchLightMode(lightModeOn)
        verify(interactor).setLightMode(lightModeOn)
    }

    @Test
    fun didTapAbout() {
        presenter.didTapAbout()
        verify(router).showAbout()
    }

    @Test
    fun didTapAppLink() {
        presenter.didTapAppLink()
        verify(router).openAppLink()
    }

    @Test
    fun didBackup() {
        val backedUp = true
        presenter.didUpdateNonBackedUp(0)

        verify(view).setBackedUp(backedUp)
        verify(view).setTabItemBadge(0)
    }

    @Test
    fun didUpdateBaseCurrency() {
        val currencyCode = "EUR"
        presenter.didUpdateBaseCurrency(currencyCode)
        verify(view).setBaseCurrency(currencyCode)
    }

    @Test
    fun didUpdateLightMode() {
        presenter.didUpdateLightMode()
        verify(router).reloadAppInterface()
    }

    @Test
    fun onClear() {
        presenter.onClear()

        verify(interactor).clear()
    }

}
