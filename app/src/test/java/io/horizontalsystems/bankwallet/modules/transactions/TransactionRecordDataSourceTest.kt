package io.horizontalsystems.bankwallet.modules.transactions

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.entities.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times

class TransactionRecordDataSourceTest {

    private val poolRepo = mock(PoolRepo::class.java)!!
    private val itemsDataSource = mock(TransactionItemDataSource::class.java)!!
    private val factory = mock(TransactionItemFactory::class.java)!!
    private val limit = 10
    private val btc = mock(Coin::class.java)
    private val bch = mock(Coin::class.java)
    private val eth = mock(Coin::class.java)

    private lateinit var dataSource: TransactionRecordDataSource

    @Before
    fun setup() {
        whenever(btc.type).thenReturn(mock(CoinType.Bitcoin::class.java))
        whenever(bch.type).thenReturn(mock(CoinType.BitcoinCash::class.java))
        whenever(eth.type).thenReturn(mock(CoinType.Ethereum::class.java))

        dataSource = TransactionRecordDataSource(poolRepo, itemsDataSource, factory, limit)
    }

    @Test
    fun setCoinCodes() {
        val coinCodes = listOf(btc, eth)
        val pool1 = mock(Pool::class.java)
        val pool2 = mock(Pool::class.java)
        val pools = listOf(pool1, pool2)

        whenever(poolRepo.allPools).thenReturn(pools)

        dataSource.setCoinCodes(coinCodes)

        verify(pool1).resetFirstUnusedIndex()
        verify(pool2).resetFirstUnusedIndex()
        verify(poolRepo).activatePools(coinCodes)
        verify(itemsDataSource).clear()
    }

    @Test
    fun getFetchDataList() {
        val pool1 = mock(Pool::class.java)
        val pool2 = mock(Pool::class.java)
        val pool3 = mock(Pool::class.java)

        val fetchData1 = mock(TransactionsModule.FetchData::class.java)
        val fetchData2 = mock(TransactionsModule.FetchData::class.java)

        whenever(poolRepo.activePools).thenReturn(listOf(pool1, pool2, pool3))
        whenever(pool1.getFetchData(limit)).thenReturn(fetchData1)
        whenever(pool2.getFetchData(limit)).thenReturn(null)
        whenever(pool3.getFetchData(limit)).thenReturn(fetchData2)

        Assert.assertArrayEquals(arrayOf(fetchData1, fetchData2), dataSource.getFetchDataList().toTypedArray())
    }

    @Test
    fun handleNextRecords() {
        val transactionRecords1 = listOf(mock(TransactionRecord::class.java))
        val coin1 = btc
        val pool1 = mock(Pool::class.java)

        val transactionRecords2 = listOf(mock(TransactionRecord::class.java))
        val coin2 = eth
        val pool2 = mock(Pool::class.java)

        val records = mapOf(coin1 to transactionRecords1, coin2 to transactionRecords2)

        whenever(poolRepo.getPool(coin1)).thenReturn(pool1)
        whenever(poolRepo.getPool(coin2)).thenReturn(pool2)

        dataSource.handleNextRecords(records)

        verify(pool1).add(transactionRecords1)
        verify(pool2).add(transactionRecords2)
    }

    @Test
    fun increasePage_inserted() {
        val dataSource = TransactionRecordDataSource(poolRepo, itemsDataSource, factory, 3)

        val address = TransactionAddress("address", false)
        val btc1Rec = TransactionRecord(
                transactionHash = "btc_1_hash",
                transactionIndex = 2,
                interTransactionIndex = 3,
                blockHeight = 0,
                amount = 3.toBigDecimal(),
                timestamp = 3,
                from = listOf(address),
                to = listOf(address)
        )

        val btc2Rec = TransactionRecord(
                transactionHash = "btc_2_hash",
                transactionIndex = 2,
                interTransactionIndex = 3,
                blockHeight = 0,
                amount = 3.toBigDecimal(),
                timestamp = 1,
                from = listOf(address),
                to = listOf(address)
        )

        val eth1Rec = TransactionRecord(
                transactionHash = "eth_1_hash",
                transactionIndex = 2,
                interTransactionIndex = 2,
                blockHeight = 0,
                amount = 3.toBigDecimal(),
                timestamp = 2,
                from = listOf(address),
                to = listOf(address)
        )

        val eth2Rec = TransactionRecord(
                transactionHash = "eth_2_hash",
                transactionIndex = 2,
                interTransactionIndex = 3,
                blockHeight = 0,
                amount = 3.toBigDecimal(),
                timestamp = 4,
                from = listOf(address),
                to = listOf(address)
        )

        val itemBtc1 = TransactionItem(btc, btc1Rec)
        val itemBtc2 = TransactionItem(btc, btc2Rec)
        val itemEth1 = TransactionItem(eth, eth1Rec)
        val itemEth2 = TransactionItem(eth, eth2Rec)

        val poolBtc = mock(Pool::class.java)
        val poolEth = mock(Pool::class.java)

        whenever(poolRepo.activePools).thenReturn(listOf(poolBtc, poolEth))
        whenever(poolRepo.getPool(btc)).thenReturn(poolBtc)
        whenever(poolRepo.getPool(eth)).thenReturn(poolEth)

        whenever(poolBtc.coin).thenReturn(btc)
        whenever(poolEth.coin).thenReturn(eth)
        whenever(poolBtc.unusedRecords).thenReturn(listOf(btc1Rec, btc2Rec))
        whenever(poolEth.unusedRecords).thenReturn(listOf(eth1Rec, eth2Rec))

        whenever(factory.createTransactionItem(btc, btc1Rec)).thenReturn(itemBtc1)
        whenever(factory.createTransactionItem(btc, btc2Rec)).thenReturn(itemBtc2)
        whenever(factory.createTransactionItem(eth, eth1Rec)).thenReturn(itemEth1)
        whenever(factory.createTransactionItem(eth, eth2Rec)).thenReturn(itemEth2)

        val result = dataSource.increasePage()

        val items = listOf(itemEth2, itemBtc1, itemEth1)

        verify(itemsDataSource).add(items)
        verify(poolBtc).increaseFirstUnusedIndex()
        verify(poolEth, times(2)).increaseFirstUnusedIndex()

        Assert.assertEquals(3, result)
    }

    @Test
    fun increasePage_zero() {


        val poolBtc = mock(Pool::class.java)
        val poolEth = mock(Pool::class.java)

        whenever(poolRepo.activePools).thenReturn(listOf(poolBtc, poolEth))
        whenever(poolRepo.getPool(btc)).thenReturn(poolBtc)
        whenever(poolRepo.getPool(eth)).thenReturn(poolEth)

        whenever(poolBtc.unusedRecords).thenReturn(listOf())
        whenever(poolEth.unusedRecords).thenReturn(listOf())

        val result = dataSource.increasePage()

        Assert.assertEquals(0, result)
    }

    @Test
    fun itemForIndex() {
        val index = 123
        val transactionItem = mock(TransactionItem::class.java)

        whenever(itemsDataSource.itemForIndex(index)).thenReturn(transactionItem)

        Assert.assertEquals(transactionItem, dataSource.itemForIndex(index))
    }

    @Test
    fun itemsCount() {
        val itemsCount = 123

        whenever(itemsDataSource.count).thenReturn(itemsCount)

        Assert.assertEquals(itemsCount, dataSource.itemsCount)
    }

    @Test
    fun itemIndexesForTimestamp() {
        val timestamp = 123123L
        val indexes = listOf(1, 3, 4)

        whenever(itemsDataSource.itemIndexesForTimestamp(btc, timestamp)).thenReturn(indexes)

        Assert.assertArrayEquals(indexes.toIntArray(), dataSource.itemIndexesForTimestamp(btc, timestamp).toIntArray())
    }

    @Test
    fun allShown_true() {
        val poolBtc = mock(Pool::class.java)
        val poolEth = mock(Pool::class.java)

        whenever(poolRepo.activePools).thenReturn(listOf(poolBtc, poolEth))
        whenever(poolBtc.allShown).thenReturn(true)
        whenever(poolEth.allShown).thenReturn(true)

        Assert.assertTrue(dataSource.allShown)
    }

    @Test
    fun allShown_false() {
        val poolBtc = mock(Pool::class.java)
        val poolEth = mock(Pool::class.java)

        whenever(poolRepo.activePools).thenReturn(listOf(poolBtc, poolEth))
        whenever(poolBtc.allShown).thenReturn(true)
        whenever(poolEth.allShown).thenReturn(false)

        Assert.assertFalse(dataSource.allShown)
    }

    @Test
    fun allRecords() {
        val poolBtc = mock(Pool::class.java)
        val poolEth = mock(Pool::class.java)

        val recordsBtc = mutableListOf(mock(TransactionRecord::class.java), mock(TransactionRecord::class.java))
        val recordsEth = mutableListOf(mock(TransactionRecord::class.java))

        whenever(poolRepo.activePools).thenReturn(listOf(poolBtc, poolEth))
        whenever(poolBtc.coin).thenReturn(btc)
        whenever(poolEth.coin).thenReturn(eth)
        whenever(poolBtc.records).thenReturn(recordsBtc)
        whenever(poolEth.records).thenReturn(recordsEth)

        val actualAllRecords = dataSource.allRecords

        Assert.assertArrayEquals(arrayOf(btc, eth), actualAllRecords.keys.toTypedArray())
        Assert.assertArrayEquals(recordsBtc.toTypedArray(), actualAllRecords[btc]?.toTypedArray())
        Assert.assertArrayEquals(recordsEth.toTypedArray(), actualAllRecords[eth]?.toTypedArray())
    }

    @Test
    fun handleUpdatedRecords_noPool() {
        val records = listOf<TransactionRecord>()


        whenever(poolRepo.getPool(btc)).thenReturn(null)

        Assert.assertNull(dataSource.handleUpdatedRecords(records, btc))
    }

    @Test
    fun handleUpdatedRecords_poolIsInactive() {
        val records = listOf<TransactionRecord>()

        val pool = mock(Pool::class.java)

        whenever(poolRepo.getPool(btc)).thenReturn(pool)
        whenever(poolRepo.isPoolActiveByCoinCode(btc)).thenReturn(false)

        Assert.assertNull(dataSource.handleUpdatedRecords(records, btc))
    }

    @Test
    fun handleUpdatedRecords_emptyModifiedLists_newData_shouldNotInsertRecord() {
        val record1 = mock(TransactionRecord::class.java)
        val records = listOf(record1)

        val pool = mock(Pool::class.java)

        whenever(poolRepo.getPool(btc)).thenReturn(pool)
        whenever(poolRepo.isPoolActiveByCoinCode(btc)).thenReturn(true)
        whenever(pool.handleUpdatedRecord(record1)).thenReturn(Pool.HandleResult.NEW_DATA)
        whenever(itemsDataSource.shouldInsertRecord(record1)).thenReturn(false)

        Assert.assertNull(dataSource.handleUpdatedRecords(records, btc))
    }

    @Test
    fun handleUpdatedRecords_newData_shouldInsertRecord() {
        val record1 = mock(TransactionRecord::class.java)
        val records = listOf(record1)

        val pool = mock(Pool::class.java)
        val transactionItem = mock(TransactionItem::class.java)

        whenever(poolRepo.getPool(btc)).thenReturn(pool)
        whenever(poolRepo.isPoolActiveByCoinCode(btc)).thenReturn(true)
        whenever(pool.handleUpdatedRecord(record1)).thenReturn(Pool.HandleResult.NEW_DATA)
        whenever(itemsDataSource.shouldInsertRecord(record1)).thenReturn(true)
        whenever(factory.createTransactionItem(btc, record1)).thenReturn(transactionItem)

        val result = dataSource.handleUpdatedRecords(records, btc)

        verify(pool).increaseFirstUnusedIndex()
        verify(itemsDataSource).handleModifiedItems(listOf(), listOf(transactionItem))

        Assert.assertNull(result)
    }

    @Test
    fun handleUpdatedRecords_emptyModifiedLists_noNewData() {
        val record1 = mock(TransactionRecord::class.java)
        val records = listOf(record1)

        val pool = mock(Pool::class.java)

        whenever(poolRepo.getPool(btc)).thenReturn(pool)
        whenever(poolRepo.isPoolActiveByCoinCode(btc)).thenReturn(true)
        whenever(pool.handleUpdatedRecord(record1)).thenReturn(Pool.HandleResult.IGNORED)

        Assert.assertNull(dataSource.handleUpdatedRecords(records, btc))
    }

    @Test
    fun handleUpdatedRecords() {

        val pool = mock(Pool::class.java)

        val updatedRecord1 = mock(TransactionRecord::class.java)
        val updatedRecord2 = mock(TransactionRecord::class.java)
        val insertedRecord1 = mock(TransactionRecord::class.java)
        val ignoredRecord = mock(TransactionRecord::class.java)
        val records = listOf<TransactionRecord>(updatedRecord1, ignoredRecord, updatedRecord2, insertedRecord1)

        val updatedItem1 = TransactionItem(btc, updatedRecord1)
        val updatedItem2 = TransactionItem(btc, updatedRecord2)
        val insertedItem1 = TransactionItem(btc, insertedRecord1)

        whenever(poolRepo.getPool(btc)).thenReturn(pool)
        whenever(poolRepo.isPoolActiveByCoinCode(btc)).thenReturn(true)
        whenever(pool.handleUpdatedRecord(updatedRecord1)).thenReturn(Pool.HandleResult.UPDATED)
        whenever(pool.handleUpdatedRecord(ignoredRecord)).thenReturn(Pool.HandleResult.IGNORED)
        whenever(pool.handleUpdatedRecord(updatedRecord2)).thenReturn(Pool.HandleResult.UPDATED)
        whenever(pool.handleUpdatedRecord(insertedRecord1)).thenReturn(Pool.HandleResult.INSERTED)
        whenever(factory.createTransactionItem(btc, updatedRecord1)).thenReturn(updatedItem1)
        whenever(factory.createTransactionItem(btc, updatedRecord2)).thenReturn(updatedItem2)
        whenever(factory.createTransactionItem(btc, insertedRecord1)).thenReturn(insertedItem1)

        dataSource.handleUpdatedRecords(records, btc)

        verify(itemsDataSource).handleModifiedItems(listOf(updatedItem1, updatedItem2), listOf(insertedItem1))
    }

    @Test
    fun itemIndexesForPending() {
        val lastBlockHeight = 100

        dataSource.itemIndexesForPending(btc, lastBlockHeight)

        verify(itemsDataSource).itemIndexesForPending(btc, lastBlockHeight)
    }
}
