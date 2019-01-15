package io.horizontalsystems.bankwallet.modules.transactions

import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.TransactionItem
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class TransactionsPresenterTest {

    private val interactor = mock(TransactionsModule.IInteractor::class.java)
    private val router = mock(TransactionsModule.IRouter::class.java)
    private val view = mock(TransactionsModule.IView::class.java)
    private val factory = mock(TransactionViewItemFactory::class.java)
    private val loader = mock(TransactionsLoader::class.java)

    private lateinit var presenter: TransactionsPresenter

    @Before
    fun before() {
        presenter = TransactionsPresenter(interactor, router, factory, loader)
        presenter.view = view
    }

    @Test
    fun itemsCount() {
        val itemsCount = 123

        whenever(loader.itemsCount).thenReturn(itemsCount)

        Assert.assertEquals(itemsCount, presenter.itemsCount)
    }

    @Test
    fun itemForIndex() {
        val index = 42
        val viewItem = mock(TransactionViewItem::class.java)
        val transactionItem = mock(TransactionItem::class.java)

        whenever(loader.itemForIndex(index)).thenReturn(transactionItem)
        whenever(factory.item(transactionItem)).thenReturn(viewItem)

        Assert.assertEquals(viewItem, presenter.itemForIndex(index))
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()

        verify(interactor).fetchCoinCodes()
    }

    @Test
    fun onFilterSelect_all() {
        presenter.onFilterSelect(null)

        verify(interactor).setSelectedCoinCodes(listOf())
    }

    @Test
    fun onFilterSelect() {
        val coinCode = "BTC"

        presenter.onFilterSelect(coinCode)

        verify(interactor).setSelectedCoinCodes(listOf(coinCode))
    }

    @Test
    fun onUpdateCoinCodes() {
        val allCoinCodes = listOf("BTC", "ETH")

        presenter.onUpdateCoinCodes(allCoinCodes)

        verify(loader).setCoinCodes(allCoinCodes)
        verify(loader).loading = false
        verify(loader).loadNext()
        verify(view).showFilters(listOf(null, "BTC", "ETH"))
    }

    @Test
    fun onUpdateSelectedCoinCodes() {
        val coinCodes = listOf("BTC")

        presenter.onUpdateSelectedCoinCodes(coinCodes)

        verify(loader).setCoinCodes(coinCodes)
        verify(loader).loading = false
        verify(loader).loadNext()
    }

    @Test
    fun didFetchRecords() {
        val records = mapOf<CoinCode, List<TransactionRecord>>("BTC" to listOf())

        presenter.didFetchRecords(records)

        verify(loader).didFetchRecords(records)
    }

    @Test
    fun bottomReached_loaderLoading() {
        whenever(loader.loading).thenReturn(true)

        presenter.onBottomReached()

        verify(loader, never()).loadNext()

    }

    @Test
    fun bottomReached_loaderNotLoading() {
        whenever(loader.loading).thenReturn(false)

        presenter.onBottomReached()

        verify(loader).loadNext()
    }

    @Test
    fun didChangeData() {
        presenter.didChangeData()

        verify(loader).loading = false
        verify(view).reload()
    }

    @Test
    fun fetchRecords() {
        val fetchDataList = listOf<TransactionsModule.FetchData>(mock(TransactionsModule.FetchData::class.java))

        presenter.fetchRecords(fetchDataList)

        verify(interactor).fetchRecords(fetchDataList)
    }

}
