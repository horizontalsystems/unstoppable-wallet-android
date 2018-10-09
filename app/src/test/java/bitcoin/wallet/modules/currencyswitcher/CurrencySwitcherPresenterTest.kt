package bitcoin.wallet.modules.currencyswitcher

import bitcoin.wallet.entities.Currency
import bitcoin.wallet.entities.CurrencyType
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
