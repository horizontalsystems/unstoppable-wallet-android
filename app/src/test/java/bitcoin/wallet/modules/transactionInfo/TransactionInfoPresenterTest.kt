package bitcoin.wallet.modules.transactionInfo

import bitcoin.wallet.modules.transactions.TransactionRecordViewItem
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify

class TransactionInfoPresenterTest {

    private val interactor = Mockito.mock(TransactionInfoModule.IInteractor::class.java)
    private val router = Mockito.mock(TransactionInfoModule.IRouter::class.java)
    private val view = Mockito.mock(TransactionInfoModule.IView::class.java)

    private val presenter = TransactionInfoPresenter(interactor, router)

    @Before
    fun setUp() {
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()

        verify(interactor).getTransactionInfo()
    }

    @Test
    fun didGetTransactionInfo() {
        val transactionRecordViewItem = Mockito.mock(TransactionRecordViewItem::class.java)
        presenter.didGetTransactionInfo(transactionRecordViewItem)

        view.showTransactionItem(transactionRecordViewItem)
    }

}
