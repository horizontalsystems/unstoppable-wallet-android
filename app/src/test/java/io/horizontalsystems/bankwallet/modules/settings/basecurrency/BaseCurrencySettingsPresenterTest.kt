package io.horizontalsystems.bankwallet.modules.settings.basecurrency

import io.horizontalsystems.bankwallet.entities.Currency
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class BaseCurrencySettingsPresenterTest {

    private val interactor = Mockito.mock(BaseCurrencySettingsModule.IBaseCurrencySettingsInteractor::class.java)
    private val view = Mockito.mock(BaseCurrencySettingsModule.IBaseCurrencySettingsView::class.java)
    private val presenter = BaseCurrencySettingsPresenter(interactor)

    private val dollarCode = "USD"
    private val rubleCode = "EUR"

    private val dollarSymbol = "$"
    private val rubleSymbol = "P"

    private val currencyUsd = Currency(code = dollarCode, symbol = dollarSymbol)
    private val currencyEur = Currency(code = rubleCode, symbol = rubleSymbol)
    private val currencies = listOf(currencyUsd, currencyEur)
    private val currencyItemSelected = CurrencyItem(code = dollarCode, symbol = dollarSymbol, selected = true)
    private val currencyItemNonSelected = CurrencyItem(code = rubleCode, symbol = rubleSymbol, selected = false)

    private val expectedItems = listOf(
            CurrencyItem(code = dollarCode, symbol = dollarSymbol, selected = true),
            CurrencyItem(code = rubleCode, symbol = rubleSymbol, selected = false)
    )

    @Before
    fun setUp() {
        presenter.view = view

        whenever(interactor.currencies).thenReturn(currencies)
        whenever(interactor.baseCurrency).thenReturn(currencyUsd)
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()
        verify(view).show(expectedItems)
    }

    @Test
    fun didSelect_nonSelected() {
        presenter.didSelect(currencyItemNonSelected)
        verify(interactor).setBaseCurrency(currencyItemNonSelected.code)
    }

    @Test
    fun didSelect_Selected() {
        presenter.didSelect(currencyItemSelected)
        verify(interactor, never()).setBaseCurrency(currencyItemSelected.code)
    }

    @Test
    fun didSetBaseCurrency_reload() {
        presenter.didSetBaseCurrency()
        verify(view).show(any())
    }
}
