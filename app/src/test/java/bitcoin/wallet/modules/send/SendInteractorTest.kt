package bitcoin.wallet.modules.send

import bitcoin.wallet.blockchain.BlockchainManager
import bitcoin.wallet.blockchain.InvalidAddress
import bitcoin.wallet.blockchain.NotEnoughFundsException
import bitcoin.wallet.core.DatabaseChangeset
import bitcoin.wallet.core.IClipboardManager
import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.entities.ExchangeRate
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
    private val blockchainManager = Mockito.mock(BlockchainManager::class.java)
    private val clipboardManager = Mockito.mock(IClipboardManager::class.java)

    private val coinCode = "BTC"
    private val interactor = SendInteractor(databaseManager, blockchainManager, clipboardManager, coinCode)

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

        val exchangeRate = ExchangeRate().apply {
            code = "BTC"
            value = 7349.4
        }
        whenever(databaseManager.getExchangeRates()).thenReturn(Observable.just(DatabaseChangeset(arrayListOf(exchangeRate))))

        interactor.fetchExchangeRate()

        verify(delegate).didFetchExchangeRate(exchangeRate.value)
    }

    @Test
    fun send() {

        val coinCode = "BTC"
        val address = "address"
        val amountBTC = 1.0
        val amountSatoshi = 100_000_000L

        interactor.send(coinCode, address, amountBTC)

        verify(blockchainManager).sendCoins(coinCode, address, amountSatoshi)
    }

    @Test
    fun send_invalidAddress() {

        val coinCode = "BTC"
        val address = "address"
        val amountBTC = 1.0
        val amountSatoshi = 100_000_000L

        val exception = InvalidAddress(Throwable())

        whenever(blockchainManager.sendCoins(coinCode, address, amountSatoshi)).thenThrow(exception)
        interactor.send(coinCode, address, amountBTC)

        verify(delegate).didFailToSend(exception)
    }

    @Test
    fun send_insufficientAmount() {

        val coinCode = "BTC"
        val address = "address"
        val amountBTC = 1.0
        val amountSatoshi = 100_000_000L

        val exception = NotEnoughFundsException(Throwable())

        whenever(blockchainManager.sendCoins(coinCode, address, amountSatoshi)).thenThrow(exception)
        interactor.send(coinCode, address, amountBTC)

        verify(delegate).didFailToSend(exception)
    }


    @Test
    fun send_success() {

        val coinCode = "BTC"
        val address = "address"
        val amountBTC = 1.0

        interactor.send(coinCode, address, amountBTC)

        verify(delegate).didSend()
    }

}
