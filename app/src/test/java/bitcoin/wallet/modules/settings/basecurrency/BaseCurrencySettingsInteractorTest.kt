package bitcoin.wallet.modules.settings.basecurrency

import bitcoin.wallet.core.ICurrencyManager
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class BaseCurrencySettingsInteractorTest {

    private val delegate = Mockito.mock(BaseCurrencySettingsModule.IBaseCurrencySettingsInteractorDelegate::class.java)
    private val currencyManager = Mockito.mock(ICurrencyManager::class.java)

    private val interactor = BaseCurrencySettingsInteractor(currencyManager)

    private val dollarCode = "USD"
    private val rubleCode = "EUR"

    private val dollarSymbol = "$"
    private val rubleSymbol = "P"

    private val currencyUsd = Currency(code = dollarCode, symbol = dollarSymbol)
    private val currencyEur = Currency(code = rubleCode, symbol = rubleSymbol)
    private val currencies = listOf(currencyUsd, currencyEur)

    @Before
    fun setUp() {
        RxBaseTest.setup()

        interactor.delegate = delegate

        whenever(interactor.currencies).thenReturn(currencies)
        whenever(interactor.baseCurrency).thenReturn(currencyUsd)
    }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun getCurrencies() {
        Assert.assertEquals(interactor.currencies, currencies)
    }

    @Test
    fun getBaseCurrency() {
        Assert.assertEquals(interactor.baseCurrency, currencyUsd)
    }

    @Test
    fun setBaseCurrency() {
        interactor.setBaseCurrency(code = rubleCode)

        verify(currencyManager).setBaseCurrency(rubleCode)
        verify(delegate).didSetBaseCurrency()
    }
}
