package bitcoin.wallet.modules.send

import bitcoin.wallet.core.BitcoinAdapter
import bitcoin.wallet.core.IClipboardManager
import bitcoin.wallet.core.IExchangeRateManager
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.entities.CurrencyType
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class SendInteractorTest {

    private val delegate = Mockito.mock(SendModule.IInteractorDelegate::class.java)
    private val clipboardManager = Mockito.mock(IClipboardManager::class.java)
    private val bitcoinAdapter = Mockito.mock(BitcoinAdapter::class.java)
    private val exchangeRateManager = Mockito.mock(IExchangeRateManager::class.java)

    private val interactor = SendInteractor(clipboardManager, bitcoinAdapter, exchangeRateManager)
    private val currency1 = Currency().apply {
        code = "USD"
        symbol = "U+0024"
        name = "US Dollar"
        type = CurrencyType.FIAT
        codeNumeric = 840
    }
    private var exchangeRates = mutableMapOf(Bitcoin() as Coin to CurrencyValue(currency1, 10_000.0))

    @Before
    fun setUp() {
        RxBaseTest.setup()
        interactor.delegate = delegate
    }

    @Test
    fun getCopiedText() {
        interactor.getCopiedText()

        verify(clipboardManager).getCopiedText()
    }

    @Test
    fun returnCopiedText() {
        val copiedText = "copied_address"

        whenever(clipboardManager.getCopiedText()).thenReturn(copiedText)

        Assert.assertEquals(copiedText, interactor.getCopiedText())
    }

    @Test
    fun fetchExchangeRate() {
        val coin = Bitcoin()

        whenever(exchangeRateManager.getExchangeRates()).thenReturn(exchangeRates)
        whenever(bitcoinAdapter.coin).thenReturn(coin)

        interactor.fetchExchangeRate()

        verify(delegate).didFetchExchangeRate(10_000.0)
    }

    @Test
    fun send() {
        val address = "address"
        val amountBTC = 1.0

        interactor.send(address, amountBTC)

        verify(bitcoinAdapter).send(address, amountBTC)
    }

//    @Test
//    fun send_invalidAddress() {
//
//        val coinCode = "BTC"
//        val address = "address"
//        val amountBTC = 1.0
//        val amountSatoshi = 100_000_000L
//
//        val exception = InvalidAddress(Throwable())
//
//        interactor.send(coinCode, address, amountBTC)
//
//        verify(delegate).didFailToSend(exception)
//    }

//    @Test
//    fun send_insufficientAmount() {
//
//        val coinCode = "BTC"
//        val address = "address"
//        val amountBTC = 1.0
//        val amountSatoshi = 100_000_000L
//
//        val exception = NotEnoughFundsException(Throwable())
//
//        interactor.send(coinCode, address, amountBTC)
//
//        verify(delegate).didFailToSend(exception)
//    }


    @Test
    fun send_success() {

        val address = "address"
        val amountBTC = 1.0

        interactor.send(address, amountBTC)

        verify(delegate).didSend()
    }

}
