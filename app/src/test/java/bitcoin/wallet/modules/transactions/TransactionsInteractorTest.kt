package bitcoin.wallet.modules.transactions

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.BitcoinAdapter
import bitcoin.wallet.core.IExchangeRateManager
import bitcoin.wallet.entities.*
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.util.*

class TransactionsInteractorTest {

    private val delegate = mock(TransactionsModule.IInteractorDelegate::class.java)
    private val exchangeRateManager = mock(IExchangeRateManager::class.java)
    private val adapterManager = mock(AdapterManager::class.java)
    private val bitcoinAdapter = mock(BitcoinAdapter::class.java)

    private var coin = Bitcoin()
    private var words = listOf("used", "ugly", "meat", "glad", "balance", "divorce", "inner", "artwork", "hire", "invest", "already", "piano")
    private var wordsHash = words.joinToString(" ")
    private var adapterId: String = "${wordsHash.hashCode()}-${coin.code}"

    private val baseCurrency = Currency().apply {
        code = "USD"
        symbol = "U+0024"
        name = "US Dollar"
        type = CurrencyType.FIAT
        codeNumeric = 840
    }
    private val baseCurrencyFlowable = Flowable.just(baseCurrency)

    private val interactor = TransactionsInteractor(adapterManager, exchangeRateManager, baseCurrencyFlowable)


    @Before
    fun before() {
        RxBaseTest.setup()

        interactor.delegate = delegate

        val rateResponse = Flowable.just(6300.0)
        whenever(exchangeRateManager.getRate(any(), any(), any())).thenReturn(rateResponse)
    }

    @Test
    fun retrieveFilters() {
        val subject: PublishSubject<Any> = PublishSubject.create()
        whenever(adapterManager.adapters).thenReturn(mutableListOf(bitcoinAdapter))
        whenever(adapterManager.subject).thenReturn(PublishSubject.create<Any>())

        whenever(bitcoinAdapter.id).thenReturn(adapterId)
        whenever(bitcoinAdapter.coin).thenReturn(coin)
        whenever(bitcoinAdapter.balance).thenReturn(0.0)
        whenever(bitcoinAdapter.transactionRecordsSubject).thenReturn(subject)

        interactor.retrieveFilters()

        verify(delegate).didRetrieveFilters(any())
    }

    @Test
    fun retrieveTransactionItems() {
        val subject: PublishSubject<Any> = PublishSubject.create()

        val transaction = TransactionRecord()
        transaction.transactionHash = "efw43f3fwer"
        transaction.coinCode = "BTC"
        transaction.from = listOf("mxNEBQf2xQeLknPZW65rMbKxEban6udxFc")
        transaction.to = listOf("13UwE8nL9PBezSrMK5LtncsTR6Er7DhBdy")
        transaction.amount = -0.23
        transaction.fee = 0.00012
        transaction.blockHeight = 125
        transaction.timestamp = 1536152151123

        whenever(adapterManager.adapters).thenReturn(mutableListOf(bitcoinAdapter))
        whenever(adapterManager.subject).thenReturn(PublishSubject.create<Any>())

        whenever(bitcoinAdapter.id).thenReturn(adapterId)
        whenever(bitcoinAdapter.coin).thenReturn(coin)
        whenever(bitcoinAdapter.balance).thenReturn(0.0)
        whenever(bitcoinAdapter.transactionRecords).thenReturn(listOf(transaction))
        whenever(bitcoinAdapter.transactionRecordsSubject).thenReturn(subject)

        interactor.retrieveTransactions(null)

        verify(delegate).didRetrieveItems(any())
    }

    @Test
    fun retrieveTransactionItems_transactionOutConvert() {
        val now = Date()
        val bitcoin = Bitcoin()
        val subject: PublishSubject<Any> = PublishSubject.create()
        val timestampNow = now.time
        val rate = 6300.0

        val btcTxAmount = 10.0

        val transactionRecordBTCsuccess = TransactionRecord().apply {
            transactionHash = "transactionHash"
            amount = btcTxAmount
            fee = 1.0
            incoming = true
            timestamp = timestampNow
            from = listOf("from-address")
            to = listOf("to-address")
            blockHeight = 98
            coinCode = "BTC"
        }

        val transactionRecordBTCpending = TransactionRecord().apply {
            transactionHash = "transactionHash"
            amount = btcTxAmount
            fee = 1.0
            incoming = true
            timestamp = now.time
            from = listOf("from-address")
            to = listOf("to-address")
            blockHeight = 101
            coinCode = "BTC"
        }

        val expectedItems = listOf(
                TransactionRecordViewItem(
                        hash = "transactionHash",
                        adapterId = adapterId,
                        amount = CoinValue(bitcoin, btcTxAmount),
                        fee = CoinValue(bitcoin, 1.0),
                        from ="from-address",
                        to = "to-address",
                        incoming = true,
                        blockHeight = 98,
                        date = now,
                        confirmations = 3,
                        currencyAmount = CurrencyValue(currency = baseCurrency, value = btcTxAmount * rate),
                        exchangeRate = rate
                ),
                TransactionRecordViewItem(
                        hash = "transactionHash",
                        adapterId = adapterId,
                        amount = CoinValue(bitcoin, btcTxAmount),
                        fee = CoinValue(bitcoin, 1.0),
                        from = "from-address",
                        to = "to-address",
                        incoming = true,
                        blockHeight = 101,
                        date = now,
                        confirmations = 0,
                        currencyAmount = CurrencyValue(currency = baseCurrency, value = btcTxAmount * rate),
                        exchangeRate = rate
                )
        )

        whenever(adapterManager.adapters).thenReturn(mutableListOf(bitcoinAdapter))
        whenever(adapterManager.subject).thenReturn(PublishSubject.create<Any>())

        whenever(bitcoinAdapter.id).thenReturn(adapterId)
        whenever(bitcoinAdapter.coin).thenReturn(coin)
        whenever(bitcoinAdapter.balance).thenReturn(0.0)
        whenever(bitcoinAdapter.latestBlockHeight).thenReturn(100)
        whenever(bitcoinAdapter.transactionRecords).thenReturn(listOf(transactionRecordBTCsuccess, transactionRecordBTCpending))
        whenever(bitcoinAdapter.transactionRecordsSubject).thenReturn(subject)

        interactor.retrieveTransactions(null)

        verify(delegate).didRetrieveItems(expectedItems)
    }

}
