package bitcoin.wallet.modules.currencyswitcher

import bitcoin.wallet.entities.Currency
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class CurrencySwitcherPresenterTest {

    private val interactor = mock(CurrencySwitcherModule.IInteractor::class.java)
    private val view = mock(CurrencySwitcherModule.IView::class.java)
    private val presenter = CurrencySwitcherPresenter(interactor)
    private val currency1 = Currency().apply {
        code = "USD"
        symbol = "$"
        name = "United States Dollar"
    }

    private val currency2 = Currency().apply {
        code = "EUR"
        symbol = "€"
        name = "Euro"
    }

    @Before
    fun setUp() {
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()

        verify(interactor).getAvailableCurrencies()
    }

    @Test
    fun currencyListUpdated() {
        val currencyViewItemList = listOf(CurrencyViewItem(currency1, true), CurrencyViewItem(currency2, false))

        presenter.currencyListUpdated(currencyViewItemList)
        verify(view).updateCurrencyList(currencyViewItemList)
    }

    @Test
    fun onItemClick() {
        presenter.onItemClick(currency1)
        verify(interactor).setBaseCurrency(currency1)
    }
}
