package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify

class TransactionInfoPresenterTest {
    private val transHash = "0123"
    private val interactor = Mockito.mock(TransactionInfoModule.Interactor::class.java)
    private val router = Mockito.mock(TransactionInfoModule.Router::class.java)
    private val transactionFactory = Mockito.mock(TransactionViewItemFactory::class.java)
    private val view = Mockito.mock(TransactionInfoModule.View::class.java)

    private val presenter = TransactionInfoPresenter(interactor, router, transactionFactory)

    @Before
    fun setUp() {
        presenter.view = view
    }


    @Test
    fun getTransaction() {
        presenter.getTransaction(transHash)

        verify(interactor).getTransaction(transHash)
    }

//    @Test
//    fun onCopyFromAddress() {
//        presenter.onCopyFromAddress()
//        verify(interactor).onTapItem()
//    }

    @Test
    fun didGetTransaction() {
//        val transactionRecord = Mockito.mock(TransactionRecord::class.java)
//        val transactionViewItem = Mockito.mock(TransactionViewItem::class.java)
//        presenter.didGetTransaction(transactionRecord)
//
//        view.showTransactionItem(transactionViewItem)
    }

}
