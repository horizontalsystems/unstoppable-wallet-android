package bitcoin.wallet.modules.currencyswitcher

import bitcoin.wallet.core.ISettingsManager
import bitcoin.wallet.core.NetworkManager
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.atLeast
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class CurrencySwitcherInteractorTest {
    private val delegate = mock(CurrencySwitcherModule.IInteractorDelegate::class.java)
    private val networkManager = mock(NetworkManager::class.java)
    private val settingsManager = mock(ISettingsManager::class.java)

    private val interactor = CurrencySwitcherInteractor(networkManager, settingsManager)

    private val currency1 = Currency().apply {
        code = "USD"
        symbol = "$"
        description = "United States Dollar"
    }

    private val currency2 = Currency().apply {
        code = "EUR"
        symbol = "€"
        description = "Euro"
    }
    private val currencyList = listOf(currency1, currency2)
    private val currencyViewItemList = listOf(CurrencyViewItem(currency1, true), CurrencyViewItem(currency2, false))


    @Before
    fun before() {
        RxBaseTest.setup()

        interactor.delegate = delegate

        whenever(networkManager.getCurrencies()).thenReturn(Flowable.just(currencyList))
        whenever(settingsManager.getBaseCurrency()).thenReturn(currency1)
    }

    @Test
    fun retrieveAvailableCurrencies() {
        interactor.getAvailableCurrencies()
        verify(delegate).currencyListUpdated(currencyViewItemList)
    }

    @Test
    fun setBaseCurrency() {

        val expectedCurrencyList= listOf(CurrencyViewItem(currency1, false), CurrencyViewItem(currency2, true))

        interactor.getAvailableCurrencies()

        interactor.setBaseCurrency(currency2)
        verify(delegate, atLeast(2)).currencyListUpdated(expectedCurrencyList)
    }

}
