package io.horizontalsystems.bankwallet.modules.transactions

import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.*
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

    private val coin1 = mock(Coin::class.java)
    private val coin2 = mock(Coin::class.java)

    private val wallet1 = mock(Wallet::class.java)
    private val wallet2 = mock(Wallet::class.java)

    private lateinit var presenter: TransactionsPresenter

    @Before
    fun before() {
        presenter = TransactionsPresenter(interactor, router, factory, loader, metadataDataSource)
        presenter.view = view

        whenever(wallet1.coin).thenReturn(coin1)
        whenever(wallet2.coin).thenReturn(coin2)
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
        val lastBlockInfo = LastBlockInfo(lastBlockHeight)
        val threshold = 6
        val viewItem = mock(TransactionViewItem::class.java)
        val transactionItem = mock(TransactionItem::class.java)
        val transactionRecord = mock(TransactionRecord::class.java)
        val timestamp = 123123L
        val rateCurrencyValue = mock(CurrencyValue::class.java)

        whenever(transactionRecord.timestamp).thenReturn(timestamp)
        whenever(transactionItem.record).thenReturn(transactionRecord)
        whenever(transactionItem.wallet).thenReturn(wallet1)
        whenever(loader.itemForIndex(index)).thenReturn(transactionItem)
        whenever(metadataDataSource.getLastBlockInfo(wallet1)).thenReturn(lastBlockInfo)
        whenever(metadataDataSource.getConfirmationThreshold(wallet1)).thenReturn(threshold)
        whenever(metadataDataSource.getRate(coin1, timestamp)).thenReturn(rateCurrencyValue)
        whenever(factory.item(wallet1, transactionItem, lastBlockInfo, threshold, rateCurrencyValue)).thenReturn(viewItem)

        val result = presenter.itemForIndex(index)

        verify(factory).item(wallet1, transactionItem, lastBlockInfo, threshold, rateCurrencyValue)
        verify(interactor, never()).fetchRate(coin1, timestamp)

        Assert.assertEquals(viewItem, result)
    }

    @Test
    fun itemForIndex_fetchRate() {
        val index = 42
        val timestamp = 123123L
        val transactionItem = mock(TransactionItem::class.java)
        val transactionRecord = mock(TransactionRecord::class.java)

        whenever(loader.itemForIndex(index)).thenReturn(transactionItem)
        whenever(transactionRecord.timestamp).thenReturn(timestamp)
        whenever(transactionItem.record).thenReturn(transactionRecord)
        whenever(transactionItem.wallet).thenReturn(wallet1)
        whenever(metadataDataSource.getRate(coin1, timestamp)).thenReturn(null)

        presenter.itemForIndex(index)

        verify(interactor).fetchRate(coin1, timestamp)
    }

    @Test
    fun onVisible() {
        presenter.onVisible()
        verify(view).reload()
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()

        verify(interactor).initialFetch()
    }

    @Test
    fun onFilterSelect_all() {
        presenter.onFilterSelect(null)

        verify(interactor).setSelectedWallets(listOf())
    }

    @Test
    fun onFilterSelect() {
        presenter.onFilterSelect(wallet1)

        verify(interactor).setSelectedWallets(listOf(wallet1))
    }

    @Test
    fun onUpdateCoinsData() {
        val confirmationThreshold1 = 6
        val lastBlockInfo1 = LastBlockInfo(123)
        val lastBlockInfo2 = null
        val confirmationThreshold2 = 12
        val allWalletsData = listOf(
                Triple(wallet1, confirmationThreshold1, lastBlockInfo1),
                Triple(wallet2, confirmationThreshold2, lastBlockInfo2)
        )

        presenter.onUpdateWalletsData(allWalletsData)

        verify(metadataDataSource).setConfirmationThreshold(confirmationThreshold1, wallet1)
        verify(metadataDataSource).setConfirmationThreshold(confirmationThreshold2, wallet2)
        verify(metadataDataSource).setLastBlockInfo(lastBlockInfo1, wallet1)
        verify(loader).loadNext(true)
        verify(view).showFilters(listOf(null, wallet1, wallet2))
        verify(interactor).fetchLastBlockHeights()
    }

    @Test
    fun onUpdateCoinsData_lessThen2Coins() {
        val confirmationThreshold1 = 6
        val lastBlockInfo = LastBlockInfo(123)
        val allCoinsData = listOf(Triple(wallet1, confirmationThreshold1, lastBlockInfo))

        presenter.onUpdateWalletsData(allCoinsData)

        verify(view).showFilters(listOf())
    }

    @Test
    fun onUpdateSelectedCoinCodes() {
        val selectedWallets = listOf(wallet1)

        presenter.onUpdateSelectedWallets(selectedWallets)

        verify(loader).setWallets(selectedWallets)
        verify(loader).loadNext(true)
    }

    @Test
    fun didFetchRecords() {
        val record1 = mock(TransactionRecord::class.java)
        val timestamp1 = 123435L
        val records = mapOf(wallet1 to listOf<TransactionRecord>(record1))

        whenever(record1.timestamp).thenReturn(timestamp1)

        presenter.didFetchRecords(records)

        verify(loader).didFetchRecords(records)
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
        val lastBlockInfo = LastBlockInfo(123123)

        whenever(metadataDataSource.getLastBlockInfo(wallet1)).thenReturn(null)
        presenter.onUpdateLastBlock(wallet1, lastBlockInfo)

        verify(metadataDataSource).setLastBlockInfo(lastBlockInfo, wallet1)
        verify(view).reload()
    }

    @Test
    fun onUpdateLastBlockHeight_threshold() {
        val lastBlockInfo = LastBlockInfo(123123)
        val oldBlockInfo = LastBlockInfo(123122)
        val threshold = 1

        whenever(metadataDataSource.getConfirmationThreshold(wallet1)).thenReturn(threshold)
        whenever(metadataDataSource.getLastBlockInfo(wallet1)).thenReturn(oldBlockInfo)
        whenever(loader.itemIndexesForPending(wallet1, oldBlockInfo.height - threshold)).thenReturn(listOf(1))

        presenter.onUpdateLastBlock(wallet1, lastBlockInfo)

        verify(metadataDataSource).setLastBlockInfo(lastBlockInfo, wallet1)
        verify(view).reloadItems(listOf(1))
    }

    @Test
    fun onUpdateBaseCurrency() {
        val record1 = mock(TransactionRecord::class.java)
        val timestamp1 = 123435L
        val transactionRecords = mapOf(wallet1 to listOf<TransactionRecord>(record1))

        whenever(record1.timestamp).thenReturn(timestamp1)
        whenever(loader.allRecords).thenReturn(transactionRecords)

        presenter.onUpdateBaseCurrency()

        inOrder(metadataDataSource, view, interactor).let {
            it.verify(metadataDataSource).clearRates()
            it.verify(view).reload()
        }
    }

    @Test
    fun didFetchRate() {
        val rateValue = 123.123.toBigDecimal()
        val currency = mock(Currency::class.java)
        val timestamp = 123345123L

        presenter.didFetchRate(rateValue, coin1, currency, timestamp)

        verify(metadataDataSource).setRate(rateValue, coin1, currency, timestamp)
    }

    @Test
    fun didFetchRate_needToUpdateViewItem() {
        val rateValue = 123.123.toBigDecimal()
        val currency = mock(Currency::class.java)
        val timestamp = 123345123L
        val itemIndexes = listOf(123)

        whenever(loader.itemIndexesForTimestamp(coin1, timestamp)).thenReturn(itemIndexes)

        presenter.didFetchRate(rateValue, coin1, currency, timestamp)

        verify(view).reloadItems(itemIndexes)
    }

    @Test
    fun didFetchRate_notNeedToUpdateViewItem() {
        val rateValue = 123.123.toBigDecimal()
        val currency = mock(Currency::class.java)
        val timestamp = 123345123L

        whenever(loader.itemIndexesForTimestamp(coin1, timestamp)).thenReturn(listOf())

        presenter.didFetchRate(rateValue, coin1, currency, timestamp)

        verifyNoMoreInteractions(view)
    }

    @Test
    fun didUpdateRecords() {
        val record = mock(TransactionRecord::class.java)
        val records = listOf(record)
        val timestamp = 123123L

        whenever(record.timestamp).thenReturn(timestamp)

        presenter.didUpdateRecords(records, wallet1)

        verify(loader).didUpdateRecords(records, wallet1)
    }

}
