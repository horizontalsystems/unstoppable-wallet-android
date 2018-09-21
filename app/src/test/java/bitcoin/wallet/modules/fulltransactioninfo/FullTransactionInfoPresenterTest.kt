package bitcoin.wallet.modules.fulltransactioninfo

import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.DollarCurrency
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.*

class FullTransactionInfoPresenterTest {

    private val interactor = Mockito.mock(FullTransactionInfoModule.IInteractor::class.java)
    private val view = Mockito.mock(FullTransactionInfoModule.IView::class.java)
    private val router = Mockito.mock(FullTransactionInfoModule.IRouter::class.java)

    private val presenter = FullTransactionInfoPresenter(interactor, router)

    private var coin = Bitcoin()
    private val transactionId = "[transaction_id]"
    private val adapterId = "[adapter_id]"
    private var exchangeRates = mapOf("BTC" to 10_000.0)
    private val btcTxAmount = 10.0
    private val now = Date()

    private val transactionRecordViewItem = TransactionRecordViewItem(
            transactionId,
            adapterId,
            CoinValue(coin, btcTxAmount),
            CoinValue(coin, 1.0),
            "from-address",
            "to-address",
            true,
            98,
            now,
            3,
            CurrencyValue(currency = DollarCurrency(), value = btcTxAmount * (exchangeRates["BTC"] ?: 0.0))
    )

    @Before
    fun setUp() {
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()
        verify(interactor).retrieveTransaction()
    }

    @Test
    fun didGetTransactionInfo() {
        presenter.didGetTransactionInfo(transactionRecordViewItem)
        verify(view).showTransactionItem(any())
    }

    @Test
    fun didCopyToClipboard() {
        presenter.didCopyToClipboard()
        verify(view).showCopied()
    }

    @Test
    fun showBlockInfo() {
        presenter.showBlockInfo(transactionRecordViewItem)
        verify(router).showBlockInfo(any())
    }

    @Test
    fun openShareDialog() {
        presenter.openShareDialog(transactionRecordViewItem)
        verify(router).shareTransaction(transactionRecordViewItem)
    }
}
