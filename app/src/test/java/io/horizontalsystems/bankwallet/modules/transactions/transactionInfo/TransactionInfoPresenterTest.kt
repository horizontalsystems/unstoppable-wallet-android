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

    private val presenter = TransactionInfoPresenter(interactor, router)

    @Before
    fun setUp() {
        presenter.view = view
    }


    @Test
    fun onCopy() {
        val value = "some string"

        presenter.onCopy(value)

        verify(interactor).onCopy(value)
        verify(view).showCopied()
    }


    @Test
    fun openFullInfo() {
        val transactionHash = "hash"
        val coinCode = "BTC"

        presenter.openFullInfo(transactionHash, coinCode)

        verify(router).openFullInfo(transactionHash, coinCode)
    }

}
