package bitcoin.wallet.modules.currencyswitcher

import bitcoin.wallet.entities.DollarCurrency
import bitcoin.wallet.entities.EuroCurrency
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class CurrencySwitcherPresenterTest {

    private val interactor = mock(CurrencySwitcherModule.IInteractor::class.java)
    private val view = mock(CurrencySwitcherModule.IView::class.java)
    private val presenter = CurrencySwitcherPresenter(interactor)


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
        val currencyViewItemList = listOf(CurrencyViewItem(DollarCurrency(), true), CurrencyViewItem(EuroCurrency(), false))

        presenter.currencyListUpdated(currencyViewItemList)
        verify(view).updateCurrencyList(currencyViewItemList)
    }

    @Test
    fun onItemClick() {
        val currency = DollarCurrency()

        presenter.onItemClick(currency)
        verify(interactor).setBaseCurrency(currency)
    }
}
