package bitcoin.wallet.modules.send

import bitcoin.wallet.R
import bitcoin.wallet.blockchain.InvalidAddress
import bitcoin.wallet.blockchain.NotEnoughFundsException
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class SendPresenterTest {

    private val interactor = Mockito.mock(SendModule.IInteractor::class.java)
    private val router = Mockito.mock(SendModule.IRouter::class.java)
    private val view = Mockito.mock(SendModule.IView::class.java)

    private val coinCode = "BTC"
    private val baseCurrency = "USD"
    private val presenter = SendPresenter(interactor, router, coinCode)


    @Before
    fun setUp() {
        presenter.view = view

        whenever(interactor.getBaseCurrency()).thenReturn(baseCurrency)
    }

    @Test
    fun onViewDidLoad() {

        presenter.onViewDidLoad()

        verify(interactor).getBaseCurrency()
        verify(view).setPrimaryCurrency(baseCurrency)
        verify(view).setPrimaryAmount(null)
        verify(view).setSecondaryAmountHint("0.0 $coinCode")
    }

    @Test
    fun onViewDidLoad_fetchExchangeRate() {
        presenter.onViewDidLoad()

        verify(interactor).fetchExchangeRate()
    }

    @Test
    fun onScanClick() {
        presenter.onScanClick()

        verify(router).startScan()
    }

    @Test
    fun onPasteClick() {
        presenter.onPasteClick()

        verify(interactor).getCopiedText()
    }

    @Test
    fun onGetCopiedText() {
        val copiedText = "234"

        whenever(interactor.getCopiedText()).thenReturn(copiedText)

        presenter.onPasteClick()

        verify(view).setAddress(copiedText)
    }

    @Test
    fun onFiatAmountEntered() {
        val exchangeRate = 7000.0
        val amountEntered = 3500.0

        val secondaryAmountHint = "0.5 BTC"

        presenter.onViewDidLoad()
        presenter.didFetchExchangeRate(exchangeRate)
        presenter.onAmountEntered(amountEntered)

        verify(view).setSecondaryAmountHint(secondaryAmountHint)
    }

    @Test
    fun onCryptoAmountEntered() {
        val exchangeRate = 7000.0
        val amountEntered = 1.0

        val secondaryAmountHint = "7000.0 USD"

        presenter.onViewDidLoad()
        presenter.didFetchExchangeRate(exchangeRate)
        presenter.onCurrencyButtonClick()

        presenter.onAmountEntered(amountEntered)

        verify(view).setSecondaryAmountHint(secondaryAmountHint)
    }


    @Test
    fun onCurrencyButtonClick_initial() {

        val exchangeRate = 7000.0
        val fiatAmount = 3500.0
        val cryptoAmount = 0.5

        val cryptoCurrency = "BTC"
        val fiatCurrency = "USD"

        presenter.onViewDidLoad()
        presenter.didFetchExchangeRate(exchangeRate)
        presenter.onAmountEntered(fiatAmount)

        presenter.onCurrencyButtonClick()


        verify(view).setPrimaryCurrency(cryptoCurrency)
        verify(view).setPrimaryAmount(cryptoAmount)
        verify(view, atLeastOnce()).setSecondaryAmountHint("$fiatAmount $fiatCurrency")
    }

    @Test
    fun onCurrencyButtonClick_afterSwap() {

        val exchangeRate = 7000.0
        val fiatAmount = 3500.0
        val cryptoAmount = 0.5

        val cryptoCurrency = "BTC"
        val fiatCurrency = "USD"

        presenter.onViewDidLoad()
        presenter.didFetchExchangeRate(exchangeRate)
        presenter.onAmountEntered(fiatAmount)

        presenter.onCurrencyButtonClick()
        reset(view)
        presenter.onCurrencyButtonClick()

        verify(view).setPrimaryCurrency(fiatCurrency)
        verify(view).setPrimaryAmount(fiatAmount)
        verify(view).setSecondaryAmountHint("$cryptoAmount $cryptoCurrency")
    }

    @Test
    fun onCancelClick() {

        presenter.onCancelClick()

        verify(view).closeView()
    }

    @Test
    fun onSendClick() {

        val exchangeRate = 7000.0
        val fiatAmount = 3500.0
        val cryptoAmount = 0.5

        val cryptoCurrencyCode = "BTC"
        val address = "mxNEBQf2xQeLknPZW65rMbKxEban6udxFc"

        presenter.onViewDidLoad()
        presenter.didFetchExchangeRate(exchangeRate)
        presenter.onAmountEntered(fiatAmount)

        presenter.onSendClick(address)

        verify(interactor).send(cryptoCurrencyCode, address, cryptoAmount)
    }

    @Test
    fun didFailToSend_invalidAddress() {
        val exception = InvalidAddress(Throwable())

        presenter.didFailToSend(exception)

        verify(view).showError(R.string.send_bottom_sheet_error_invalid_address)
    }

    @Test
    fun didFailToSend_insufficientBalance() {
        val exception = NotEnoughFundsException(Throwable())

        presenter.didFailToSend(exception)

        verify(view).showError(R.string.send_bottom_sheet_error_insufficient_balance)
    }

    @Test
    fun didSend() {

        presenter.didSend()

        verify(view).showSuccess()
    }

}
