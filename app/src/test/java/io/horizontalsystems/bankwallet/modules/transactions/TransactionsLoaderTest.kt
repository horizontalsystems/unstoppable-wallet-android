package io.horizontalsystems.bankwallet.modules.transactions

import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
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
        val coinCodes = listOf("ABC", "DEF")

        loader.setCoinCodes(coinCodes)

        verify(dataSource).setCoinCodes(coinCodes)
    }

    @Test
    fun loadNextInitial_dataSourceAllShown() {
        whenever(dataSource.allShown).thenReturn(true)

        loader.loadNext(true)

        verify(dataSource).allShown
        verify(delegate).didChangeData()
        verifyNoMoreInteractions(dataSource)
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun loadNextNotInitial_dataSourceAllShown() {
        whenever(dataSource.allShown).thenReturn(true)

        loader.loadNext(false)

        verify(dataSource).allShown
        verify(delegate, never()).didChangeData()
        verifyNoMoreInteractions(dataSource)
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun loadNext_dataSourceFetchDataListIsEmpty_didInsertData() {
        whenever(dataSource.allShown).thenReturn(false)
        whenever(dataSource.getFetchDataList()).thenReturn(listOf())

        whenever(dataSource.itemsCount).thenReturn(4)
        whenever(dataSource.increasePage()).thenReturn(10)

        loader.loadNext(false)

        verify(dataSource).increasePage()
        verify(delegate).didInsertData(4, 10)
    }

    @Test
    fun loadNext_dataSourceFetchDataListIsEmpty_dataNotChanged() {
        whenever(dataSource.allShown).thenReturn(false)
        whenever(dataSource.getFetchDataList()).thenReturn(listOf())

        whenever(dataSource.increasePage()).thenReturn(0)

        loader.loadNext(false)

        verify(dataSource).increasePage()
        verify(delegate, never()).didChangeData()
    }

    @Test
    fun loadNext_dataSourceFetchDataListIsNotEmpty() {
        val fetchDataList = listOf<TransactionsModule.FetchData>(mock(TransactionsModule.FetchData::class.java))
        whenever(dataSource.allShown).thenReturn(false)
        whenever(dataSource.getFetchDataList()).thenReturn(fetchDataList)

        loader.loadNext(false)

        verify(delegate).fetchRecords(fetchDataList)
        verifyNoMoreInteractions(delegate)

        verify(dataSource, never()).increasePage()
    }

    @Test
    fun didFetchRecords_dataInserted() {
        val records = mapOf<CoinCode, List<TransactionRecord>>("BTC" to listOf())

        whenever(dataSource.itemsCount).thenReturn(123)
        whenever(dataSource.increasePage()).thenReturn(10)

        loader.didFetchRecords(records)

        verify(dataSource).handleNextRecords(records)
        verify(dataSource).increasePage()
        verify(delegate).didInsertData(123, 10)
    }

    @Test
    fun didFetchRecords_dataNotChanged() {
        val records = mapOf<CoinCode, List<TransactionRecord>>("BTC" to listOf())

        whenever(dataSource.increasePage()).thenReturn(0)

        loader.didFetchRecords(records)

        verify(dataSource).handleNextRecords(records)
        verify(dataSource).increasePage()
        verify(delegate, never()).didChangeData()
    }

    @Test
    fun itemIndexesForTimestamp() {
        val coinCode = "BTC"
        val timestamp = 123123L
        val indexes = listOf(1231, 12323)

        whenever(dataSource.itemIndexesForTimestamp(coinCode, timestamp)).thenReturn(indexes)

        Assert.assertEquals(indexes, loader.itemIndexesForTimestamp(coinCode, timestamp))
    }

    @Test
    fun didUpdateRecords_reloadRequired() {
        val records = listOf(mock(TransactionRecord::class.java))
        val coinCode = "BTC"

        whenever(dataSource.handleUpdatedRecords(records, coinCode)).thenReturn(true)

        loader.didUpdateRecords(records, coinCode)

        verify(delegate).didChangeData()
    }

    @Test
    fun didUpdateRecords_reloadNotRequired() {
        val records = listOf(mock(TransactionRecord::class.java))
        val coinCode = "BTC"

        whenever(dataSource.handleUpdatedRecords(records, coinCode)).thenReturn(false)

        loader.didUpdateRecords(records, coinCode)

        verify(delegate, never()).didChangeData()
    }

    @Test
    fun itemIndexesForPending() {
        val coinCode = "BTC"
        val lastBlockHeight = 100
        val threshold = 6

        loader.itemIndexesForPending(coinCode, lastBlockHeight, threshold)

        verify(dataSource).itemIndexesForPending(coinCode, lastBlockHeight, threshold)
    }
}
