package bitcoin.wallet.modules.transactions

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.BitcoinAdapter
import bitcoin.wallet.core.ExchangeRateManager
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.DollarCurrency
import bitcoin.wallet.entities.TransactionRecordNew
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.util.*

class TransactionsInteractorTest {

    private val delegate = mock(TransactionsModule.IInteractorDelegate::class.java)
    private val exchangeRateManager = mock(ExchangeRateManager::class.java)
    private val adapterManager = mock(AdapterManager::class.java)
    private val bitcoinAdapter = mock(BitcoinAdapter::class.java)

    private var coin = Bitcoin()
    private var words = listOf("used", "ugly", "meat", "glad", "balance", "divorce", "inner", "artwork", "hire", "invest", "already", "piano")
    private var wordsHash = words.joinToString(" ")
    private var adapterId: String = "${wordsHash.hashCode()}-${coin.code}"

    private val interactor = TransactionsInteractor(adapterManager, exchangeRateManager)

    private var exchangeRates = mapOf("BTC" to 10_000.0)

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor.delegate = delegate

        whenever(exchangeRateManager.exchangeRates).thenReturn(exchangeRates)
    }

    @Test
    fun retrieveFilters() {
        val subject: PublishSubject<Void> = PublishSubject.create()
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
        val subject: PublishSubject<Void> = PublishSubject.create()

        val transaction = TransactionRecordNew()
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

        interactor.retrieveTransactionItems()

        verify(delegate).didRetrieveItems(any())
    }


    @Test
    fun retrieveTransactionItems_transactionOutConvert() {
        val now = Date()
        val bitcoin = Bitcoin()
        val subject: PublishSubject<Void> = PublishSubject.create()

        val btcTxAmount = 10.0

        val transactionRecordBTCsuccess = TransactionRecordNew().apply {
            transactionHash = "transactionHash"
            amount = btcTxAmount
            fee = 1.0
            incoming = true
            timestamp = now.time
            from = listOf("from-address")
            to = listOf("to-address")
            blockHeight = 98
            coinCode = "BTC"
        }

        val transactionRecordBTCpending = TransactionRecordNew().apply {
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
                        "transactionHash",
                        CoinValue(bitcoin, btcTxAmount),
                        CoinValue(bitcoin, 1.0),
                        "from-address",
                        "to-address",
                        true,
                        98,
                        now,
                        TransactionRecordViewItem.Status.SUCCESS,
                        3,
                        CurrencyValue(currency = DollarCurrency(), value = btcTxAmount * (exchangeRates["BTC"] ?: 0.0))
                ),
                TransactionRecordViewItem(
                        "transactionHash",
                        CoinValue(bitcoin, btcTxAmount),
                        CoinValue(bitcoin, 1.0),
                        "from-address",
                        "to-address",
                        true,
                        101,
                        now,
                        TransactionRecordViewItem.Status.PENDING,
                        0,
                        CurrencyValue(currency = DollarCurrency(), value = btcTxAmount * (exchangeRates["BTC"] ?: 0.0))
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

        interactor.retrieveTransactionItems()

        verify(delegate).didRetrieveItems(expectedItems)
    }

}
