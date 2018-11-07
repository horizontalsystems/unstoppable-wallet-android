package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.core.BitcoinAdapter
import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.IExchangeRateManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.coins.bitcoin.Bitcoin
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.*

class FullTransactionInfoInteractorTest {

    private val delegate = mock(FullTransactionInfoModule.IInteractorDelegate::class.java)
    private val clipboardManager = mock(IClipboardManager::class.java)
    private val exchangeRateManager = mock(IExchangeRateManager::class.java)
    private val bitcoinAdapter = mock(BitcoinAdapter::class.java)
    private var coin = Bitcoin()
    private val transactionId = "[transaction_id]"
    private val btcTxAmount = 10.0
    private val now = Date()

    private val transaction = TransactionRecord().apply {
        transactionHash = transactionId
        amount = btcTxAmount
        fee = 1.0
        timestamp = now.time
        from = listOf("from-address")
        to = listOf("to-address")
        blockHeight = 98
        coinCode = "BTC"
    }

    private val currencyUsd = Currency(code = "USD", symbol = "\u0024")

    private val interactor = FullTransactionInfoInteractor(bitcoinAdapter, exchangeRateManager, transactionId, clipboardManager, currencyUsd)


    @Before
    fun before() {
        RxBaseTest.setup()

        interactor.delegate = delegate

        val rateResponse = Flowable.just(6000.0)
        whenever(exchangeRateManager.getRate(any(), any(), any())).thenReturn(rateResponse)

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
