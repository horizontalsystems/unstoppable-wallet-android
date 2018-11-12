package io.horizontalsystems.bankwallet.modules.transactions

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.BitcoinAdapter
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IExchangeRateManager
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.coins.bitcoin.Bitcoin
import io.horizontalsystems.bankwallet.modules.RxBaseTest
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
    private val adapterManager = mock(IAdapterManager::class.java)
    private val currencyManager = mock(ICurrencyManager::class.java)
    private val bitcoinAdapter = mock(BitcoinAdapter::class.java)

    private var coin = Bitcoin()
    private var words = listOf("used", "ugly", "meat", "glad", "balance", "divorce", "inner", "artwork", "hire", "invest", "already", "piano")
    private var wordsHash = words.joinToString(" ")
    private var adapterId: String = "${wordsHash.hashCode()}-${coin.code}"
    private val baseCurrency = Currency(code = "USD", symbol = "\u0024")


    private val subject: PublishSubject<Currency> = PublishSubject.create()

    private lateinit var interactor: TransactionsInteractor


    @Before
    fun before() {
        RxBaseTest.setup()

        val rateResponse = Flowable.just(6300.0)
        whenever(exchangeRateManager.getRate(any(), any(), any())).thenReturn(rateResponse)
        whenever(currencyManager.baseCurrency).thenReturn(baseCurrency)
        whenever(currencyManager.subject).thenReturn(subject)

        interactor = TransactionsInteractor(adapterManager, exchangeRateManager, currencyManager)

        interactor.delegate = delegate
    }

    @Test
    fun retrieveFilters() {
        val subject: PublishSubject<Any> = PublishSubject.create()
        whenever(adapterManager.adapters).thenReturn(mutableListOf(bitcoinAdapter))
        whenever(adapterManager.subject).thenReturn(PublishSubject.create<Boolean>())

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
        whenever(adapterManager.subject).thenReturn(PublishSubject.create<Boolean>())

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
            timestamp = timestampNow
            from = listOf("from-address")
            to = listOf("to-address")
            blockHeight = 98
            status = TransactionStatus.Processing(33)
            coinCode = "BTC"
        }

        val transactionRecordBTCpending = TransactionRecord().apply {
            transactionHash = "transactionHash"
            amount = btcTxAmount
            fee = 1.0
            timestamp = now.time
            from = listOf("from-address")
            to = listOf("to-address")
            blockHeight = 101
            coinCode = "BTC"
            status = TransactionStatus.Pending
        }

        val expectedItems = listOf(
                TransactionRecordViewItem(
                        hash = "transactionHash",
                        adapterId = adapterId,
                        amount = CoinValue(bitcoin, btcTxAmount),
                        fee = CoinValue(bitcoin, 1.0),
                        from = "from-address",
                        to = "to-address",
                        incoming = true,
                        blockHeight = 98,
                        date = now,
                        status = TransactionStatus.Processing(33),
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
                        status = TransactionStatus.Pending,
                        currencyAmount = CurrencyValue(currency = baseCurrency, value = btcTxAmount * rate),
                        exchangeRate = rate
                )
        )

        whenever(adapterManager.adapters).thenReturn(mutableListOf(bitcoinAdapter))
        whenever(adapterManager.subject).thenReturn(PublishSubject.create<Boolean>())

        whenever(bitcoinAdapter.id).thenReturn(adapterId)
        whenever(bitcoinAdapter.coin).thenReturn(coin)
        whenever(bitcoinAdapter.balance).thenReturn(0.0)
        whenever(bitcoinAdapter.transactionRecords).thenReturn(listOf(transactionRecordBTCsuccess, transactionRecordBTCpending))
        whenever(bitcoinAdapter.transactionRecordsSubject).thenReturn(subject)

        interactor.retrieveTransactions(null)

        verify(delegate).didRetrieveItems(expectedItems)
    }

}
