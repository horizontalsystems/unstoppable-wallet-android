package bitcoin.wallet.modules.settings.main

import bitcoin.wallet.core.*
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.entities.CurrencyType
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.*

class MainSettingsInteractorTest {

    private val delegate = mock(MainSettingsModule.IMainSettingsInteractorDelegate::class.java)

    private lateinit var localStorage: ILocalStorage
    private lateinit var wordsManager: IWordsManager
    private lateinit var languageManager: ILanguageManager
    private lateinit var sysInfoManager: ISystemInfoManager
    private lateinit var currencyManager: ICurrencyManager

    private lateinit var interactor: MainSettingsInteractor

    private val backedUpSubject = PublishSubject.create<Boolean>()

    val currentLanguage : Locale = Locale("en")

    private val currency = Currency().apply {
        code = "USD"
        symbol = "U+0024"
        name = "US Dollar"
        type = CurrencyType.FIAT
        codeNumeric = 840
    }

    private val appVersion = "1,01"

    @Before
    fun setUp() {
        RxBaseTest.setup()

        wordsManager = mock {
            on { isBackedUp } doReturn true
            on { backedUpSubject } doReturn backedUpSubject
        }

        localStorage = mock {
            on { isLightModeOn } doReturn true
            on { baseCurrency } doReturn currency
        }

        languageManager = mock{
            on { currentLanguage } doReturn currentLanguage
        }

        sysInfoManager = mock{
            on { appVersion } doReturn appVersion
        }

        currencyManager = mock{
            on { getBaseCurrencyFlowable() } doReturn Flowable.just(currency)
        }


        interactor = MainSettingsInteractor(localStorage, wordsManager, languageManager, sysInfoManager, currencyManager)

        interactor.delegate = delegate
    }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(delegate)
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
        interactor = MainSettingsInteractor(localStorage, wordsManager, languageManager, sysInfoManager, currencyManager)
        interactor.delegate = delegate

        Assert.assertFalse(interactor.isBackedUp)
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
            on { baseCurrency } doReturn currency
        }
        interactor = MainSettingsInteractor(localStorage, wordsManager, languageManager, sysInfoManager, currencyManager)
        interactor.delegate = delegate

        Assert.assertFalse(interactor.getLightMode())
    }

    @Test
    fun setLightMode() {
        interactor.setLightMode(true)
        verify(localStorage).isLightModeOn = true
        verify(delegate).didUpdateLightMode()
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
