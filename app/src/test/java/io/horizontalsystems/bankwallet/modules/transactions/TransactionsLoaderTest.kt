package io.horizontalsystems.bankwallet.modules.transactions

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bankwallet.entities.TransactionItem
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class TransactionsLoaderTest {

    private val dataSource = mock(TransactionRecordDataSource::class.java)
    private val delegate = mock(TransactionsLoader.Delegate::class.java)
    private lateinit var loader: TransactionsLoader

    @Before
    fun setup() {
        loader = TransactionsLoader(dataSource)
        loader.delegate = delegate
    }

    @Test
    fun getItemsCount() {
        val itemsCount = 234

        whenever(dataSource.itemsCount).thenReturn(itemsCount)

        Assert.assertEquals(itemsCount, loader.itemsCount)
    }

    @Test
    fun getLoading() {
    }

    @Test
    fun setLoading() {
    }

    @Test
    fun itemForIndex() {
        val index = 234
        val item = mock(TransactionItem::class.java)

        whenever(dataSource.itemForIndex(index)).thenReturn(item)

        Assert.assertEquals(item, loader.itemForIndex(index))
    }

    @Test
    fun setCoinCodes() {
        val coinCodes = listOf<CoinCode>("ABC", "DEF")

        loader.setCoinCodes(coinCodes)

        verify(dataSource).setCoinCodes(coinCodes)
    }

    @Test
    fun loadNext_dataSourceAllShown() {
        whenever(dataSource.allShown).thenReturn(true)

        loader.loadNext()

        verify(dataSource).allShown
        verifyNoMoreInteractions(dataSource)
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun loadNext_dataSourceFetchDataListIsEmpty() {
        whenever(dataSource.allShown).thenReturn(false)
        whenever(dataSource.getFetchDataList()).thenReturn(listOf())

        loader.loadNext()

        inOrder(dataSource, delegate).let {
            it.verify(dataSource).increasePage()
            it.verify(delegate).didChangeData()
        }
    }

    @Test
    fun loadNext_dataSourceFetchDataListIsNotEmpty() {
        val fetchDataList = listOf<TransactionsModule.FetchData>(mock(TransactionsModule.FetchData::class.java))
        whenever(dataSource.allShown).thenReturn(false)
        whenever(dataSource.getFetchDataList()).thenReturn(fetchDataList)

        loader.loadNext()

        verify(delegate).fetchRecords(fetchDataList)
        verifyNoMoreInteractions(delegate)

        verify(dataSource, never()).increasePage()
    }

    @Test
    fun didFetchRecords() {
        val records = mapOf<CoinCode, List<TransactionRecord>>("BTC" to listOf())

        loader.didFetchRecords(records)

        verify(dataSource).handleNextRecords(records)
        verify(dataSource).increasePage()
        verify(delegate).didChangeData()

    }
}