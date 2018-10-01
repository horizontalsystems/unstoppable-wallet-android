package bitcoin.wallet.modules.fulltransactioninfo

import bitcoin.wallet.core.BitcoinAdapter
import bitcoin.wallet.core.ExchangeRateManager
import bitcoin.wallet.core.IClipboardManager
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.*
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.*

class FullTransactionInfoInteractorTest {

    private val delegate = Mockito.mock(FullTransactionInfoModule.IInteractorDelegate::class.java)
    private val clipboardManager = Mockito.mock(IClipboardManager::class.java)
    private val exchangeRateManager = Mockito.mock(ExchangeRateManager::class.java)
    private val bitcoinAdapter = Mockito.mock(BitcoinAdapter::class.java)
    private var coin = Bitcoin()
    private val transactionId = "[transaction_id]"
    private var exchangeRates = mutableMapOf("BTC" to 6_000.0)
    private val btcTxAmount = 10.0
    private val now = Date()

    private val transaction = TransactionRecord().apply {
        transactionHash = transactionId
        amount = btcTxAmount
        fee = 1.0
        incoming = true
        timestamp = now.time
        from = listOf("from-address")
        to = listOf("to-address")
        blockHeight = 98
        coinCode = "BTC"
    }

    private val interactor = FullTransactionInfoInteractor(bitcoinAdapter, transactionId, clipboardManager)


    @Before
    fun before() {
        RxBaseTest.setup()

        interactor.delegate = delegate

        whenever(exchangeRateManager.exchangeRates).thenReturn(exchangeRates)
        whenever(bitcoinAdapter.transactionRecords).thenReturn(listOf(transaction))
        whenever(bitcoinAdapter.transactionRecordsSubject).thenReturn(PublishSubject.create())
        whenever(bitcoinAdapter.coin).thenReturn(coin)
        whenever(bitcoinAdapter.id).thenReturn("adapter_id")
    }

    @Test
    fun retrieveTransaction() {
        whenever(bitcoinAdapter.transactionRecords).thenReturn(listOf(transaction))
        whenever(bitcoinAdapter.transactionRecordsSubject).thenReturn(PublishSubject.create())
        whenever(bitcoinAdapter.coin).thenReturn(coin)

        interactor.retrieveTransaction()

        verify(delegate, atLeastOnce()).didGetTransactionInfo(any())
    }

    @Test
    fun getTransactionInfo() {
        interactor.retrieveTransaction()

        interactor.getTransactionInfo()
        verify(delegate, atLeast(2)).didGetTransactionInfo(any())
    }

    @Test
    fun onCopyFromAddress() {
        interactor.retrieveTransaction()

        interactor.onCopyFromAddress()
        verify(clipboardManager).copyText(any())
        verify(delegate).didCopyToClipboard()
    }

    @Test
    fun onCopyToAddress() {
        interactor.retrieveTransaction()

        interactor.onCopyToAddress()
        verify(clipboardManager).copyText(any())
        verify(delegate).didCopyToClipboard()
    }

    @Test
    fun onCopyTransactionId() {
        interactor.retrieveTransaction()

        interactor.onCopyTransactionId()
        verify(clipboardManager).copyText(any())
        verify(delegate).didCopyToClipboard()
    }

    @Test
    fun showFullInfo() {
        interactor.retrieveTransaction()

        interactor.showBlockInfo()
        verify(delegate).showBlockInfo(any())
    }

    @Test
    fun openShareDialog() {
        interactor.retrieveTransaction()
        interactor.openShareDialog()
        verify(delegate).openShareDialog(any())
    }

    @Test
    fun transactionsUpdated() {
        val subject: PublishSubject<Any> = PublishSubject.create()
        whenever(bitcoinAdapter.transactionRecordsSubject).thenReturn(subject)

        interactor.retrieveTransaction()
        verify(delegate, atLeastOnce()).didGetTransactionInfo(any())

        subject.onNext(Any())
        verify(delegate, atLeastOnce()).didGetTransactionInfo(any())
    }

}
