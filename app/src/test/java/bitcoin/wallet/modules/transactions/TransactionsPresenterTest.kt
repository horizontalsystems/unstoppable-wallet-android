package bitcoin.wallet.modules.transactions

import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.DollarCurrency
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import com.nhaarman.mockito_kotlin.any
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.util.*

class TransactionsPresenterTest {

    private val interactor = mock(TransactionsModule.IInteractor::class.java)
    private val router = mock(TransactionsModule.IRouter::class.java)
    private val view = mock(TransactionsModule.IView::class.java)

    private val presenter = TransactionsPresenter(interactor, router)

    @Before
    fun before() {
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()

        verify(interactor).retrieveFilters()
    }

    @Test
    fun onFilterSelect() {
        val adapterId = "[adapter_id]"
        presenter.onFilterSelect(adapterId)

        verify(interactor).retrieveTransactionItems(adapterId)
    }

    @Test
    fun didRetrieveItems() {
        val items = listOf<TransactionRecordViewItem>()

        presenter.didRetrieveItems(items)

        verify(view).showTransactionItems(items)
    }

    @Test
    fun didRetrieveFilters() {
        presenter.didRetrieveFilters(listOf())

        verify(view).showFilters(any())
    }

    @Test
    fun onTransactionItemClick() {
        val coinCode = "BTC"
        val txHash = "tx_hash"
        val transactionRecord = TransactionRecordViewItem(
                "",
                CoinValue(Bitcoin(), 0.0),
                CoinValue(Bitcoin(), 0.0),
                "",
                "",
                true,
                0,
                Date(),
                TransactionRecordViewItem.Status.SUCCESS,
                0,
                CurrencyValue(DollarCurrency(), 0.0),
                0.0
                )

        presenter.onTransactionItemClick(transactionRecord)

        verify(router).showTransactionInfo(transactionRecord)
    }
}
