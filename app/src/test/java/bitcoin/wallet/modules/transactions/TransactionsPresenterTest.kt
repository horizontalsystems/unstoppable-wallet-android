package bitcoin.wallet.modules.transactions

import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

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

        verify(interactor).retrieveTransactionRecords()
    }

    @Test
    fun didRetrieveTransactionRecords() {
        val items = listOf<TransactionRecordViewItem>()

        presenter.didRetrieveTransactionRecords(items)

        verify(view).showTransactionItems(items)
    }

    @Test
    fun onTransactionItemClick() {
        val coinCode = "BTC"
        val txHash = "tx_hash"

        presenter.onTransactionItemClick(coinCode, txHash)

        verify(router).showTransactionInfo(coinCode, txHash)
    }
}
