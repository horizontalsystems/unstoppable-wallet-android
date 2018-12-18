package io.horizontalsystems.bankwallet.modules.transactions

import com.nhaarman.mockito_kotlin.any
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class TransactionsPresenterTest {

    private val interactor = mock(TransactionsModule.IInteractor::class.java)
    private val router = mock(TransactionsModule.IRouter::class.java)
    private val view = mock(TransactionsModule.IView::class.java)
    private val factory = mock(TransactionViewItemFactory::class.java)

    private lateinit var presenter: TransactionsPresenter

    @Before
    fun before() {
        presenter = TransactionsPresenter(interactor, router, factory)
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()

        verify(interactor).retrieveFilters()
    }

    @Test
    fun onFilterSelect() {
        presenter.onFilterSelect("BTC")

        verify(interactor).setCoin("BTC")
    }

    @Test
    fun didRetrieveFilters() {
        presenter.didRetrieveFilters(listOf())

        verify(view).showFilters(any())
    }

}
