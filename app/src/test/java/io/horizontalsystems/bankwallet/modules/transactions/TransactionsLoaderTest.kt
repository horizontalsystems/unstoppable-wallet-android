package io.horizontalsystems.bankwallet.modules.transactions

import androidx.recyclerview.widget.DiffUtil
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.entities.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class TransactionsLoaderTest {

    private val dataSource = mock(TransactionRecordDataSource::class.java)
    private val delegate = mock(TransactionsLoader.Delegate::class.java)
    private val diffResult = mock(DiffUtil.DiffResult::class.java)

    private val btc = mock(Coin::class.java)
    private val eth = mock(Coin::class.java)

    private val btcWallet = mock(Wallet::class.java)
    private val ethWallet = mock(Wallet::class.java)

    private lateinit var loader: TransactionsLoader

    @Before
    fun setup() {
        whenever(btc.type).thenReturn(mock(CoinType.Bitcoin::class.java))
        whenever(eth.type).thenReturn(mock(CoinType.Ethereum::class.java))

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
    fun itemForIndex() {
        val index = 234
        val item = mock(TransactionItem::class.java)

        whenever(dataSource.itemForIndex(index)).thenReturn(item)

        Assert.assertEquals(item, loader.itemForIndex(index))
    }

    @Test
    fun setCoinCodes() {
        val wallets = listOf(btcWallet, ethWallet)

        loader.setWallets(wallets)

        verify(dataSource).setWallets(wallets)
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
        val records = mapOf<Wallet, List<TransactionRecord>>(btcWallet to listOf())

        whenever(dataSource.itemsCount).thenReturn(123)
        whenever(dataSource.increasePage()).thenReturn(10)

        loader.didFetchRecords(records)

        verify(dataSource).handleNextRecords(records)
        verify(dataSource).increasePage()
        verify(delegate).didInsertData(123, 10)
    }

    @Test
    fun didFetchRecords_dataNotChanged() {
        val records = mapOf<Wallet, List<TransactionRecord>>(btcWallet to listOf())

        whenever(dataSource.increasePage()).thenReturn(0)

        loader.didFetchRecords(records)

        verify(dataSource).handleNextRecords(records)
        verify(dataSource).increasePage()
        verify(delegate, never()).didChangeData()
    }

    @Test
    fun itemIndexesForTimestamp() {
        val coin = btc
        val timestamp = 123123L
        val indexes = listOf(1231, 12323)

        whenever(dataSource.itemIndexesForTimestamp(coin, timestamp)).thenReturn(indexes)

        Assert.assertEquals(indexes, loader.itemIndexesForTimestamp(coin, timestamp))
    }

    @Test
    fun didUpdateRecords_reloadRequired() {
        val records = listOf(mock(TransactionRecord::class.java))

        whenever(dataSource.handleUpdatedRecords(records, btcWallet)).thenReturn(diffResult)

        loader.didUpdateRecords(records, btcWallet)

        verify(delegate).onChange(diffResult)
    }

    @Test
    fun didUpdateRecords_reloadNotRequired() {
        val records = listOf(mock(TransactionRecord::class.java))

        whenever(dataSource.handleUpdatedRecords(records, btcWallet)).thenReturn(null)

        loader.didUpdateRecords(records, btcWallet)

        verify(delegate, never()).didChangeData()
    }

    @Test
    fun itemIndexesForPending() {
        val lastBlockHeight = 100

        loader.itemIndexesForPending(btcWallet, lastBlockHeight)

        verify(dataSource).itemIndexesForPending(btcWallet, lastBlockHeight)
    }
}
