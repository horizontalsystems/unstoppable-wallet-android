package io.horizontalsystems.bankwallet.modules.transactionInfo

import com.nhaarman.mockito_kotlin.any
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify

class TransactionInfoPresenterTest {
    private val transHash = "0123"
    private val interactor = Mockito.mock(TransactionInfoModule.IInteractor::class.java)
    private val router = Mockito.mock(TransactionInfoModule.IRouter::class.java)
    private val transactionFactory = Mockito.mock(TransactionViewItemFactory::class.java)
    private val view = Mockito.mock(TransactionInfoModule.IView::class.java)

    private val presenter = TransactionInfoPresenter(transHash, interactor, router, transactionFactory)

    @Before
    fun setUp() {
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()

        verify(interactor).getTransaction(any())
    }

    @Test
    fun didGetTransaction() {
//        val transactionRecord = Mockito.mock(TransactionRecord::class.java)
//        val transactionViewItem = Mockito.mock(TransactionViewItem::class.java)
//        presenter.didGetTransaction(transactionRecord)
//
//        view.showTransactionItem(transactionViewItem)
    }

}
