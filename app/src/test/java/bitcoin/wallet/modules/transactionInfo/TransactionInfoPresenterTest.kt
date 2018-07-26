package bitcoin.wallet.modules.transactionInfo

import bitcoin.wallet.modules.transactions.TransactionRecordViewItem
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class TransactionInfoPresenterTest {

    private val interactor = Mockito.mock(TransactionInfoModule.IInteractor::class.java)
    private val router = Mockito.mock(TransactionInfoModule.IRouter::class.java)
    private val view = Mockito.mock(TransactionInfoModule.IView::class.java)

    private val coinCode = "BTC"
    private val txHash = "tx_hash"
    private val presenter = TransactionInfoPresenter(interactor, router, coinCode, txHash)

    @Before
    fun setUp() {
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()

        verify(interactor).getTransactionInfo(coinCode, txHash)
    }

    @Test
    fun didGetTransactionInfo() {
        val transactionRecordViewItem = Mockito.mock(TransactionRecordViewItem::class.java)
        presenter.didGetTransactionInfo(transactionRecordViewItem)

        view.showTransactionItem(transactionRecordViewItem)
    }

    @Test
    fun onMoreLessClick_expand() {

        presenter.onLessMoreClick()

        verify(view).expand()
    }

    @Test
    fun onMoreLessClick_lessen() {

        presenter.onLessMoreClick()
        reset(view)
        presenter.onLessMoreClick()

        verify(view).lessen()
    }

}
