package bitcoin.wallet.modules.currencyswitcher

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.managers.NetworkManager
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.entities.CurrencyType
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
    private val iLocalStorage = mock(ILocalStorage::class.java)

    private val interactor = CurrencySwitcherInteractor(networkManager, iLocalStorage)

    private val currency1 = Currency().apply {
        code = "USD"
        symbol = "U+0024"
        name = "US Dollar"
        type = CurrencyType.FIAT
        codeNumeric = 840
    }

    private val currency2 = Currency().apply {
        code = "EUR"
        symbol = "U+20AC"
        name = "Euro"
        type = CurrencyType.FIAT
        codeNumeric = 978
    }
    private val currencyList = listOf(currency1, currency2)
    private val currencyViewItemList = listOf(CurrencyViewItem(currency1, true), CurrencyViewItem(currency2, false))


    @Before
    fun before() {
        RxBaseTest.setup()

        interactor.delegate = delegate

        whenever(networkManager.getCurrencies()).thenReturn(Flowable.just(currencyList))
        whenever(iLocalStorage.baseCurrency).thenReturn(currency1)
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
