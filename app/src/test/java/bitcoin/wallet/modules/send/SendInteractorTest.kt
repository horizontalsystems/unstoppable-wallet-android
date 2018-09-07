package bitcoin.wallet.modules.send

import bitcoin.wallet.core.BitcoinAdapter
import bitcoin.wallet.core.DatabaseChangeset
import bitcoin.wallet.core.IClipboardManager
import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.entities.ExchangeRate
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class SendInteractorTest {

    private val delegate = Mockito.mock(SendModule.IInteractorDelegate::class.java)
    private val databaseManager = Mockito.mock(IDatabaseManager::class.java)
    private val clipboardManager = Mockito.mock(IClipboardManager::class.java)
    private val bitcoinAdapter = Mockito.mock(BitcoinAdapter::class.java)

    private val interactor = SendInteractor(databaseManager, clipboardManager, bitcoinAdapter)

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

        val exchangeRate = ExchangeRate().apply {
            code = "BTC"
            value = 7349.4
        }
        whenever(databaseManager.getExchangeRates()).thenReturn(Observable.just(DatabaseChangeset(arrayListOf(exchangeRate))))
        whenever(bitcoinAdapter.coin).thenReturn(coin)

        interactor.fetchExchangeRate()

        verify(delegate).didFetchExchangeRate(exchangeRate.value)
    }

    @Test
    fun send() {
        val coinCode = "BTC"
        val address = "address"
        val amountBTC = 1.0
        val amountSatoshi = 100_000_000

        interactor.send(coinCode, address, amountBTC)

        verify(bitcoinAdapter).send(address, amountSatoshi)
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

        val coinCode = "BTC"
        val address = "address"
        val amountBTC = 1.0

        interactor.send(coinCode, address, amountBTC)

        verify(delegate).didSend()
    }

}
