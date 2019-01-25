package io.horizontalsystems.bankwallet.modules.transactions

import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
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
    private val metadataDataSource = mock(TransactionMetadataDataSource::class.java)

    private lateinit var presenter: TransactionsPresenter

    @Before
    fun before() {
        presenter = TransactionsPresenter(interactor, router, factory, loader, metadataDataSource)
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
        val lastBlockHeight = 123
        val threshold = 6
        val coinCode = "BTC"
        val viewItem = mock(TransactionViewItem::class.java)
        val transactionItem = mock(TransactionItem::class.java)
        val transactionRecord = mock(TransactionRecord::class.java)
        val timestamp = 123123L
        val rateCurrencyValue = mock(CurrencyValue::class.java)

        whenever(transactionRecord.timestamp).thenReturn(timestamp)
        whenever(transactionItem.record).thenReturn(transactionRecord)
        whenever(transactionItem.coinCode).thenReturn(coinCode)
        whenever(loader.itemForIndex(index)).thenReturn(transactionItem)
        whenever(metadataDataSource.getLastBlockHeight(coinCode)).thenReturn(lastBlockHeight)
        whenever(metadataDataSource.getConfirmationThreshold(coinCode)).thenReturn(threshold)
        whenever(metadataDataSource.getRate(coinCode, timestamp)).thenReturn(rateCurrencyValue)
        whenever(factory.item(transactionItem, lastBlockHeight, threshold, rateCurrencyValue)).thenReturn(viewItem)

        Assert.assertEquals(viewItem, presenter.itemForIndex(index))
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()

        verify(interactor).initialFetch()
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
    fun onUpdateCoinsData() {
        val coinCode1 = "BTC"
        val coinCode2 = "ETH"
        val confirmationThreshold1 = 6
        val lastBlockHeight1 = 123
        val confirmationThreshold2 = 12
        val lastBlockHeight2 = null
        val allCoinsData = listOf(
                Triple(coinCode1, confirmationThreshold1, lastBlockHeight1),
                Triple(coinCode2, confirmationThreshold2, lastBlockHeight2)
        )

        presenter.onUpdateCoinsData(allCoinsData)

        verify(loader).setCoinCodes(listOf(coinCode1, coinCode2))
        verify(metadataDataSource).setConfirmationThreshold(confirmationThreshold1, coinCode1)
        verify(metadataDataSource).setConfirmationThreshold(confirmationThreshold2, coinCode2)
        verify(metadataDataSource).setLastBlockHeight(lastBlockHeight1, coinCode1)
        verify(loader).loadNext(true)
        verify(view).showFilters(listOf(null, coinCode1, coinCode2))
        verify(interactor).fetchLastBlockHeights()
    }

    @Test
    fun onUpdateSelectedCoinCodes() {
        val coinCodes = listOf("BTC")

        presenter.onUpdateSelectedCoinCodes(coinCodes)

        verify(loader).setCoinCodes(coinCodes)
        verify(loader).loadNext(true)
    }

    @Test
    fun didFetchRecords() {
        val coinCode1 = "BTC"
        val record1 = mock(TransactionRecord::class.java)
        val timestamp1 = 123435L
        val timestamps = listOf(timestamp1)
        val records = mapOf(coinCode1 to listOf<TransactionRecord>(record1))

        whenever(record1.timestamp).thenReturn(timestamp1)

        presenter.didFetchRecords(records)

        verify(loader).didFetchRecords(records)
        verify(interactor).fetchRates(mapOf(coinCode1 to timestamps))
    }

    @Test
    fun bottomReached() {
        presenter.onBottomReached()

        verify(loader).loadNext(false)
    }

    @Test
    fun didChangeData() {
        presenter.didChangeData()

        verify(view).reload()
    }

    @Test
    fun fetchRecords() {
        val fetchDataList = listOf<TransactionsModule.FetchData>(mock(TransactionsModule.FetchData::class.java))

        presenter.fetchRecords(fetchDataList)

        verify(interactor).fetchRecords(fetchDataList)
    }

    @Test
    fun onUpdateLastBlockHeight() {
        val coinCode = "coinCode"
        val lastBlockHeight = 123123

        presenter.onUpdateLastBlockHeight(coinCode, lastBlockHeight)

        verify(metadataDataSource).setLastBlockHeight(lastBlockHeight, coinCode)
        verify(view).reload()
    }

    @Test
    fun onUpdateBaseCurrency() {
        val coinCode1 = "BTC"
        val record1 = mock(TransactionRecord::class.java)
        val timestamp1 = 123435L
        val timestamps = listOf(timestamp1)
        val transactionRecords = mapOf(coinCode1 to listOf<TransactionRecord>(record1))

        whenever(record1.timestamp).thenReturn(timestamp1)
        whenever(loader.allRecords).thenReturn(transactionRecords)

        presenter.onUpdateBaseCurrency()

        inOrder(metadataDataSource, view, interactor).let {
            it.verify(metadataDataSource).clearRates()
            it.verify(view).reload()
            it.verify(interactor).fetchRates(mapOf(coinCode1 to timestamps))
        }
    }

    @Test
    fun didFetchRate() {
        val rateValue = 123.123
        val coinCode = "BTC"
        val currency = mock(Currency::class.java)
        val timestamp = 123345123L

        presenter.didFetchRate(rateValue, coinCode, currency, timestamp)

        verify(metadataDataSource).setRate(rateValue, coinCode, currency, timestamp)
    }

    @Test
    fun didFetchRate_needToUpdateViewItem() {
        val rateValue = 123.123
        val coinCode = "BTC"
        val currency = mock(Currency::class.java)
        val timestamp = 123345123L
        val itemIndexes = listOf(123)

        whenever(loader.itemIndexesForTimestamp(coinCode, timestamp)).thenReturn(itemIndexes)

        presenter.didFetchRate(rateValue, coinCode, currency, timestamp)

        verify(view).reloadItems(itemIndexes)
    }

    @Test
    fun didFetchRate_notNeedToUpdateViewItem() {
        val rateValue = 123.123
        val coinCode = "BTC"
        val currency = mock(Currency::class.java)
        val timestamp = 123345123L

        whenever(loader.itemIndexesForTimestamp(coinCode, timestamp)).thenReturn(listOf())

        presenter.didFetchRate(rateValue, coinCode, currency, timestamp)

        verifyNoMoreInteractions(view)
    }

    @Test
    fun didUpdateRecords() {
        val record = mock(TransactionRecord::class.java)
        val records = listOf(record)
        val coinCode = "BTC"
        val timestamp = 123123L

        whenever(record.timestamp).thenReturn(timestamp)

        presenter.didUpdateRecords(records, coinCode)

        verify(loader).didUpdateRecords(records, coinCode)
        verify(interactor).fetchRates(mapOf(coinCode to listOf(timestamp)))
    }

}
