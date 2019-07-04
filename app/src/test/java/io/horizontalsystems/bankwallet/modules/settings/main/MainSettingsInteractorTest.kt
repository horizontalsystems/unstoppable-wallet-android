package io.horizontalsystems.bankwallet.modules.settings.main

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.Flowable
import io.reactivex.Observable
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*

class MainSettingsInteractorTest {

    private val delegate = mock<MainSettingsModule.IMainSettingsInteractorDelegate>()

    private lateinit var localStorage: ILocalStorage
    private lateinit var accountManager: IAccountManager
    private lateinit var languageManager: ILanguageManager
    private lateinit var sysInfoManager: ISystemInfoManager
    private lateinit var currencyManager: ICurrencyManager

    private lateinit var interactor: MainSettingsInteractor

    private val backedUpSignal = Flowable.empty<Int>()
    private val baseCurrencyUpdatedSignal = Observable.empty<Unit>()

    val currentLanguage: Locale = Locale("en")

    private val currency = Currency(code = "USD", symbol = "\u0024")

    private val appVersion = "1,01"

    @Before
    fun setUp() {
        RxBaseTest.setup()

        accountManager = mock {
            on { nonBackedUpCountFlowable } doReturn backedUpSignal
        }

        localStorage = mock {
            on { isLightModeOn } doReturn true
            on { baseCurrencyCode } doReturn currency.code
        }

        languageManager = mock {
            on { currentLanguage } doReturn currentLanguage
        }

        sysInfoManager = mock {
            on { appVersion } doReturn appVersion
        }

        currencyManager = mock {
            on { baseCurrencyUpdatedSignal } doReturn baseCurrencyUpdatedSignal
            on { baseCurrency } doReturn currency
        }


        interactor = MainSettingsInteractor(localStorage, accountManager, languageManager, sysInfoManager, currencyManager)

        interactor.delegate = delegate
    }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun getCurrentLanguage() {
        Assert.assertEquals(interactor.currentLanguage, currentLanguage.displayLanguage)
    }

    @Test
    fun getBaseCurrency() {
        Assert.assertEquals(interactor.baseCurrency, currency.code)
    }

    @Test
    fun getAppVersion() {
        Assert.assertEquals(interactor.appVersion, appVersion)
    }

    @Test
    fun getLightMode() {
        Assert.assertTrue(interactor.getLightMode())
    }

    @Test
    fun getLightModeOff() {
        localStorage = mock {
            on { isLightModeOn } doReturn false
            on { baseCurrencyCode } doReturn currency.code
        }
        interactor = MainSettingsInteractor(localStorage, accountManager, languageManager, sysInfoManager, currencyManager)
        interactor.delegate = delegate

        Assert.assertFalse(interactor.getLightMode())
    }

    @Test
    fun setLightMode() {
        interactor.setLightMode(true)
        verify(localStorage).isLightModeOn = true
        verify(delegate).didUpdateLightMode()
    }

}
