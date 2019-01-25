package io.horizontalsystems.bankwallet.modules.transactions

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.entities.TransactionItem
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times

class TransactionRecordDataSourceTest {

    private val poolRepo = mock(PoolRepo::class.java)!!
    private val itemsDataSource = mock(TransactionItemDataSource::class.java)!!
    private val factory = mock(TransactionItemFactory::class.java)!!
    private val limit = 10

    private val dataSource = TransactionRecordDataSource(poolRepo, itemsDataSource, factory, limit)

    @Test
    fun setCoinCodes() {
        val coinCodes = listOf("BTC", "ETH")
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
        val coinCode1 = "BTC"
        val transactionRecords1 = listOf(mock(TransactionRecord::class.java))
        val pool1 = mock(Pool::class.java)

        val coinCode2 = "ETH"
        val transactionRecords2 = listOf(mock(TransactionRecord::class.java))
        val pool2 = mock(Pool::class.java)

        val records = mapOf(coinCode1 to transactionRecords1, coinCode2 to transactionRecords2)

        whenever(poolRepo.getPool(coinCode1)).thenReturn(pool1)
        whenever(poolRepo.getPool(coinCode2)).thenReturn(pool2)

        dataSource.handleNextRecords(records)

        verify(pool1).add(transactionRecords1)
        verify(pool2).add(transactionRecords2)
    }

    @Test
    fun increasePage_inserted() {
        val dataSource = TransactionRecordDataSource(poolRepo, itemsDataSource, factory, 3)

        val coinCodeBtc = "BTC"
        val coinCodeEth = "ETH"

        val recordBtc1 = mock(TransactionRecord::class.java)
        val recordBtc2 = mock(TransactionRecord::class.java)
        val recordEth1 = mock(TransactionRecord::class.java)
        val recordEth2 = mock(TransactionRecord::class.java)

        val itemBtc1 = TransactionItem(coinCodeBtc, recordBtc1)
        val itemBtc2 = TransactionItem(coinCodeBtc, recordBtc2)
        val itemEth1 = TransactionItem(coinCodeEth, recordEth1)
        val itemEth2 = TransactionItem(coinCodeEth, recordEth2)

        val poolBtc = mock(Pool::class.java)
        val poolEth = mock(Pool::class.java)

        whenever(recordBtc1.timestamp).thenReturn(3)
        whenever(recordBtc2.timestamp).thenReturn(1)
        whenever(recordEth1.timestamp).thenReturn(2)
        whenever(recordEth2.timestamp).thenReturn(4)

        whenever(poolRepo.activePools).thenReturn(listOf(poolBtc, poolEth))
        whenever(poolRepo.getPool(coinCodeBtc)).thenReturn(poolBtc)
        whenever(poolRepo.getPool(coinCodeEth)).thenReturn(poolEth)

        whenever(poolBtc.coinCode).thenReturn(coinCodeBtc)
        whenever(poolEth.coinCode).thenReturn(coinCodeEth)
        whenever(poolBtc.unusedRecords).thenReturn(listOf(recordBtc1, recordBtc2))
        whenever(poolEth.unusedRecords).thenReturn(listOf(recordEth1, recordEth2))

        whenever(factory.createTransactionItem(coinCodeBtc, recordBtc1)).thenReturn(itemBtc1)
        whenever(factory.createTransactionItem(coinCodeBtc, recordBtc2)).thenReturn(itemBtc2)
        whenever(factory.createTransactionItem(coinCodeEth, recordEth1)).thenReturn(itemEth1)
        whenever(factory.createTransactionItem(coinCodeEth, recordEth2)).thenReturn(itemEth2)

        val result = dataSource.increasePage()

        val items = listOf(itemEth2, itemBtc1, itemEth1)

        verify(itemsDataSource).add(items)
        verify(poolBtc).increaseFirstUnusedIndex()
        verify(poolEth, times(2)).increaseFirstUnusedIndex()

        Assert.assertEquals(3, result)
    }

    @Test
    fun increasePage_zero() {
        val coinCodeBtc = "BTC"
        val coinCodeEth = "ETH"

        val poolBtc = mock(Pool::class.java)
        val poolEth = mock(Pool::class.java)

        whenever(poolRepo.activePools).thenReturn(listOf(poolBtc, poolEth))
        whenever(poolRepo.getPool(coinCodeBtc)).thenReturn(poolBtc)
        whenever(poolRepo.getPool(coinCodeEth)).thenReturn(poolEth)

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
        val coinCode = "BTC"
        val timestamp = 123123L
        val indexes = listOf(1, 3, 4)

        whenever(itemsDataSource.itemIndexesForTimestamp(coinCode, timestamp)).thenReturn(indexes)

        Assert.assertArrayEquals(indexes.toIntArray(), dataSource.itemIndexesForTimestamp(coinCode, timestamp).toIntArray())
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
        val coinCodeBtc = "BTC"
        val coinCodeEth = "ETH"
        val recordsBtc = mutableListOf(mock(TransactionRecord::class.java), mock(TransactionRecord::class.java))
        val recordsEth = mutableListOf(mock(TransactionRecord::class.java))

        whenever(poolRepo.activePools).thenReturn(listOf(poolBtc, poolEth))
        whenever(poolBtc.coinCode).thenReturn(coinCodeBtc)
        whenever(poolEth.coinCode).thenReturn(coinCodeEth)
        whenever(poolBtc.records).thenReturn(recordsBtc)
        whenever(poolEth.records).thenReturn(recordsEth)

        val actualAllRecords = dataSource.allRecords

        Assert.assertArrayEquals(arrayOf(coinCodeBtc, coinCodeEth), actualAllRecords.keys.toTypedArray())
        Assert.assertArrayEquals(recordsBtc.toTypedArray(), actualAllRecords[coinCodeBtc]?.toTypedArray())
        Assert.assertArrayEquals(recordsEth.toTypedArray(), actualAllRecords[coinCodeEth]?.toTypedArray())
    }

    @Test
    fun handleUpdatedRecords_noPool() {
        val records = listOf<TransactionRecord>()
        val coinCodeBtc = "BTC"

        whenever(poolRepo.getPool(coinCodeBtc)).thenReturn(null)

        Assert.assertFalse(dataSource.handleUpdatedRecords(records, coinCodeBtc))

    }

    @Test
    fun handleUpdatedRecords_poolIsInactive() {
        val records = listOf<TransactionRecord>()
        val coinCodeBtc = "BTC"
        val pool = mock(Pool::class.java)

        whenever(poolRepo.getPool(coinCodeBtc)).thenReturn(pool)
        whenever(poolRepo.isPoolActiveByCoinCode(coinCodeBtc)).thenReturn(false)

        val result = dataSource.handleUpdatedRecords(records, coinCodeBtc)

        Assert.assertFalse(result)
    }

    @Test
    fun handleUpdatedRecords_emptyModifiedLists_newData_shouldNotInsertRecord() {
        val record1 = mock(TransactionRecord::class.java)
        val records = listOf(record1)
        val coinCodeBtc = "BTC"
        val pool = mock(Pool::class.java)

        whenever(poolRepo.getPool(coinCodeBtc)).thenReturn(pool)
        whenever(poolRepo.isPoolActiveByCoinCode(coinCodeBtc)).thenReturn(true)
        whenever(pool.handleUpdatedRecord(record1)).thenReturn(Pool.HandleResult.NEW_DATA)
        whenever(itemsDataSource.shouldInsertRecord(record1)).thenReturn(false)

        val result = dataSource.handleUpdatedRecords(records, coinCodeBtc)

        Assert.assertTrue(result)
    }

    @Test
    fun handleUpdatedRecords_newData_shouldInsertRecord() {
        val record1 = mock(TransactionRecord::class.java)
        val records = listOf(record1)
        val coinCodeBtc = "BTC"
        val pool = mock(Pool::class.java)
        val transactionItem = mock(TransactionItem::class.java)

        whenever(poolRepo.getPool(coinCodeBtc)).thenReturn(pool)
        whenever(poolRepo.isPoolActiveByCoinCode(coinCodeBtc)).thenReturn(true)
        whenever(pool.handleUpdatedRecord(record1)).thenReturn(Pool.HandleResult.NEW_DATA)
        whenever(itemsDataSource.shouldInsertRecord(record1)).thenReturn(true)
        whenever(factory.createTransactionItem(coinCodeBtc, record1)).thenReturn(transactionItem)

        val result = dataSource.handleUpdatedRecords(records, coinCodeBtc)

        verify(pool).increaseFirstUnusedIndex()
        verify(itemsDataSource).handleModifiedItems(listOf(), listOf(transactionItem))

        Assert.assertTrue(result)
    }

    @Test
    fun handleUpdatedRecords_emptyModifiedLists_noNewData() {
        val record1 = mock(TransactionRecord::class.java)
        val records = listOf(record1)
        val coinCodeBtc = "BTC"
        val pool = mock(Pool::class.java)

        whenever(poolRepo.getPool(coinCodeBtc)).thenReturn(pool)
        whenever(poolRepo.isPoolActiveByCoinCode(coinCodeBtc)).thenReturn(true)
        whenever(pool.handleUpdatedRecord(record1)).thenReturn(Pool.HandleResult.IGNORED)

        val result = dataSource.handleUpdatedRecords(records, coinCodeBtc)

        Assert.assertFalse(result)
    }

    @Test
    fun handleUpdatedRecords() {
        val coinCodeBtc = "BTC"
        val pool = mock(Pool::class.java)

        val updatedRecord1 = mock(TransactionRecord::class.java)
        val updatedRecord2 = mock(TransactionRecord::class.java)
        val insertedRecord1 = mock(TransactionRecord::class.java)
        val ignoredRecord = mock(TransactionRecord::class.java)
        val records = listOf<TransactionRecord>(updatedRecord1, ignoredRecord, updatedRecord2, insertedRecord1)

        val updatedItem1 = TransactionItem(coinCodeBtc, updatedRecord1)
        val updatedItem2 = TransactionItem(coinCodeBtc, updatedRecord2)
        val insertedItem1 = TransactionItem(coinCodeBtc, insertedRecord1)

        whenever(poolRepo.getPool(coinCodeBtc)).thenReturn(pool)
        whenever(poolRepo.isPoolActiveByCoinCode(coinCodeBtc)).thenReturn(true)
        whenever(pool.handleUpdatedRecord(updatedRecord1)).thenReturn(Pool.HandleResult.UPDATED)
        whenever(pool.handleUpdatedRecord(ignoredRecord)).thenReturn(Pool.HandleResult.IGNORED)
        whenever(pool.handleUpdatedRecord(updatedRecord2)).thenReturn(Pool.HandleResult.UPDATED)
        whenever(pool.handleUpdatedRecord(insertedRecord1)).thenReturn(Pool.HandleResult.INSERTED)
        whenever(factory.createTransactionItem(coinCodeBtc, updatedRecord1)).thenReturn(updatedItem1)
        whenever(factory.createTransactionItem(coinCodeBtc, updatedRecord2)).thenReturn(updatedItem2)
        whenever(factory.createTransactionItem(coinCodeBtc, insertedRecord1)).thenReturn(insertedItem1)

        val result = dataSource.handleUpdatedRecords(records, coinCodeBtc)

        verify(itemsDataSource).handleModifiedItems(listOf(updatedItem1, updatedItem2), listOf(insertedItem1))

        Assert.assertTrue(result)
    }
}