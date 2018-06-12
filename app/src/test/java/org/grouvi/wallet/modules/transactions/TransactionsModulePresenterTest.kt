package org.grouvi.wallet.modules.transactions

import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class TransactionsModulePresenterTest {

    private val presenter = TransactionsModulePresenter()
    private val interactor = mock(TransactionsModule.IInteractor::class.java)
    private val view = mock(TransactionsModule.IView::class.java)

    @Before
    fun before() {
        presenter.interactor = interactor
        presenter.view = view
    }

    @Test
    fun start() {
        presenter.start()

        verify(interactor).retrieveTransactionItems()
    }

    @Test
    fun didTransactionItemsRetrieve() {
        val items = listOf<TransactionViewItem>()

        presenter.didTransactionItemsRetrieve(items)

        verify(view).showItems(items)
    }
}
