package bitcoin.wallet.modules.currencyswitcher

import bitcoin.wallet.core.NetworkManager
import bitcoin.wallet.core.managers.PreferencesManager
import bitcoin.wallet.entities.DollarCurrency
import bitcoin.wallet.entities.EuroCurrency
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
    private val preferencesManager = mock(PreferencesManager::class.java)

    private val interactor = CurrencySwitcherInteractor(networkManager, preferencesManager)
    private val currencyList = listOf(DollarCurrency(), EuroCurrency())
    private val currencyViewItemList = listOf(CurrencyViewItem(DollarCurrency(), true), CurrencyViewItem(EuroCurrency(), false))


    @Before
    fun before() {
        RxBaseTest.setup()

        interactor.delegate = delegate

        whenever(networkManager.getCurrencyCodes()).thenReturn(Flowable.just(currencyList))

        val currentBaseCurrencyCode = "USD"
        whenever(preferencesManager.getBaseCurrencyCode()).thenReturn(currentBaseCurrencyCode)
    }

    @Test
    fun retrieveAvailableCurrencies() {
        interactor.getAvailableCurrencies()
        verify(delegate).currencyListUpdated(currencyViewItemList)
    }

    @Test
    fun setBaseCurrency() {
        val newBaseCurrency = EuroCurrency()

        val expectedCurrencyList= listOf(CurrencyViewItem(DollarCurrency(), false), CurrencyViewItem(EuroCurrency(), true))

        interactor.getAvailableCurrencies()

        interactor.setBaseCurrency(newBaseCurrency)
        verify(delegate, atLeast(2)).currencyListUpdated(expectedCurrencyList)
    }

}
