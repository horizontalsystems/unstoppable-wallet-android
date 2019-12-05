package io.horizontalsystems.bankwallet.modules.transactions

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.entities.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class TransactionRecordDataSourceTest {

    private val poolRepo = mock(PoolRepo::class.java)!!
    private val itemsDataSource = mock(TransactionItemDataSource::class.java)!!
    private val factory = mock(TransactionItemFactory::class.java)!!
    private val limit = 10
    private val walletBtc = mock(Wallet::class.java)
    private val walletEth = mock(Wallet::class.java)

    private lateinit var dataSource: TransactionRecordDataSource

    @Before
    fun setup() {
        dataSource = TransactionRecordDataSource(poolRepo, itemsDataSource, factory, limit)
    }

    @Test
    fun setCoinCodes() {
        val wallets = listOf(walletBtc, walletEth)
        val pool1 = mock(Pool::class.java)
        val pool2 = mock(Pool::class.java)
        val pools = listOf(pool1, pool2)

        whenever(poolRepo.allPools).thenReturn(pools)

        dataSource.setWallets(wallets)

        verify(pool1).resetFirstUnusedIndex()
        verify(pool2).resetFirstUnusedIndex()
        verify(poolRepo).activatePools(wallets)
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
        val wallet1 = walletBtc
        val pool1 = mock(Pool::class.java)

        val transactionRecords2 = listOf(mock(TransactionRecord::class.java))
        val wallet2 = walletEth
        val pool2 = mock(Pool::class.java)

        val records = mapOf(wallet1 to transactionRecords1, wallet2 to transactionRecords2)

        whenever(poolRepo.getPool(wallet1)).thenReturn(pool1)
        whenever(poolRepo.getPool(wallet2)).thenReturn(pool2)

        dataSource.handleNextRecords(records)

        verify(pool1).add(transactionRecords1)
        verify(pool2).add(transactionRecords2)
    }

    @Test
    fun increasePage_inserted() {
        val dataSource = TransactionRecordDataSource(poolRepo, itemsDataSource, factory, 3)

        val addressFrom = "addressFrom"
        val addressTo = "addressTo"

        val btc1Rec = TransactionRecord(
                uid = "uid1",
                transactionHash = "btc_1_hash",
                transactionIndex = 2,
                interTransactionIndex = 3,
                blockHeight = 0,
                amount = 3.toBigDecimal(),
                timestamp = 3,
                from = addressFrom,
                to = addressTo,
                type = TransactionType.Outgoing
        )

        val btc2Rec = TransactionRecord(
                uid = "uid2",
                transactionHash = "btc_2_hash",
                transactionIndex = 2,
                interTransactionIndex = 3,
                blockHeight = 0,
                amount = 3.toBigDecimal(),
                timestamp = 1,
                from = addressFrom,
                to = addressTo,
                type = TransactionType.Outgoing
        )

        val eth1Rec = TransactionRecord(
                uid = "uid3",
                transactionHash = "eth_1_hash",
                transactionIndex = 2,
                interTransactionIndex = 2,
                blockHeight = 0,
                amount = 3.toBigDecimal(),
                timestamp = 2,
                from = addressFrom,
                to = addressTo,
                type = TransactionType.Outgoing
        )

        val eth2Rec = TransactionRecord(
                uid = "uid4",
                transactionHash = "eth_2_hash",
                transactionIndex = 2,
                interTransactionIndex = 3,
                blockHeight = 0,
                amount = 3.toBigDecimal(),
                timestamp = 4,
                from = addressFrom,
                to = addressTo,
                type = TransactionType.Outgoing
        )

        val itemBtc1 = TransactionItem(walletBtc, btc1Rec)
        val itemBtc2 = TransactionItem(walletBtc, btc2Rec)
        val itemEth1 = TransactionItem(walletEth, eth1Rec)
        val itemEth2 = TransactionItem(walletEth, eth2Rec)

        val poolBtc = mock(Pool::class.java)
        val poolEth = mock(Pool::class.java)

        whenever(poolRepo.activePools).thenReturn(listOf(poolBtc, poolEth))
        whenever(poolRepo.getPool(walletBtc)).thenReturn(poolBtc)
        whenever(poolRepo.getPool(walletEth)).thenReturn(poolEth)

        whenever(poolBtc.wallet).thenReturn(walletBtc)
        whenever(poolEth.wallet).thenReturn(walletEth)
        whenever(poolBtc.unusedRecords).thenReturn(listOf(btc1Rec, btc2Rec))
        whenever(poolEth.unusedRecords).thenReturn(listOf(eth1Rec, eth2Rec))

        whenever(factory.createTransactionItem(walletBtc, btc1Rec)).thenReturn(itemBtc1)
        whenever(factory.createTransactionItem(walletBtc, btc2Rec)).thenReturn(itemBtc2)
        whenever(factory.createTransactionItem(walletEth, eth1Rec)).thenReturn(itemEth1)
        whenever(factory.createTransactionItem(walletEth, eth2Rec)).thenReturn(itemEth2)

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
        whenever(poolRepo.getPool(walletBtc)).thenReturn(poolBtc)
        whenever(poolRepo.getPool(walletEth)).thenReturn(poolEth)

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
        val coin = mock(Coin::class.java)

        whenever(itemsDataSource.itemIndexesForTimestamp(coin, timestamp)).thenReturn(indexes)

        Assert.assertArrayEquals(indexes.toIntArray(), dataSource.itemIndexesForTimestamp(coin, timestamp).toIntArray())
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
        whenever(poolBtc.wallet).thenReturn(walletBtc)
        whenever(poolEth.wallet).thenReturn(walletEth)
        whenever(poolBtc.records).thenReturn(recordsBtc)
        whenever(poolEth.records).thenReturn(recordsEth)

        val actualAllRecords = dataSource.allRecords

        Assert.assertArrayEquals(arrayOf(walletBtc, walletEth), actualAllRecords.keys.toTypedArray())
        Assert.assertArrayEquals(recordsBtc.toTypedArray(), actualAllRecords[walletBtc]?.toTypedArray())
        Assert.assertArrayEquals(recordsEth.toTypedArray(), actualAllRecords[walletEth]?.toTypedArray())
    }

    @Test
    fun handleUpdatedRecords_noPool() {
        val records = listOf<TransactionRecord>()

        whenever(poolRepo.getPool(walletBtc)).thenReturn(null)

        Assert.assertNull(dataSource.handleUpdatedRecords(records, walletBtc))
    }

    @Test
    fun handleUpdatedRecords_poolIsInactive() {
        val records = listOf<TransactionRecord>()

        val pool = mock(Pool::class.java)

        whenever(poolRepo.getPool(walletBtc)).thenReturn(pool)
        whenever(poolRepo.isPoolActiveByWallet(walletBtc)).thenReturn(false)

        Assert.assertNull(dataSource.handleUpdatedRecords(records, walletBtc))
    }

    @Test
    fun handleUpdatedRecords_emptyModifiedLists_newData_shouldNotInsertRecord() {
        val record1 = mock(TransactionRecord::class.java)
        val records = listOf(record1)

        val pool = mock(Pool::class.java)

        whenever(poolRepo.getPool(walletBtc)).thenReturn(pool)
        whenever(poolRepo.isPoolActiveByWallet(walletBtc)).thenReturn(true)
        whenever(pool.handleUpdatedRecord(record1)).thenReturn(Pool.HandleResult.NEW_DATA)
        whenever(itemsDataSource.shouldInsertRecord(record1)).thenReturn(false)

        Assert.assertNull(dataSource.handleUpdatedRecords(records, walletBtc))
    }

    @Test
    fun handleUpdatedRecords_newData_shouldInsertRecord() {
        val record1 = mock(TransactionRecord::class.java)
        val records = listOf(record1)

        val pool = mock(Pool::class.java)
        val transactionItem = mock(TransactionItem::class.java)

        whenever(poolRepo.getPool(walletBtc)).thenReturn(pool)
        whenever(poolRepo.isPoolActiveByWallet(walletBtc)).thenReturn(true)
        whenever(pool.handleUpdatedRecord(record1)).thenReturn(Pool.HandleResult.NEW_DATA)
        whenever(itemsDataSource.shouldInsertRecord(record1)).thenReturn(true)
        whenever(factory.createTransactionItem(walletBtc, record1)).thenReturn(transactionItem)

        val result = dataSource.handleUpdatedRecords(records, walletBtc)

        verify(pool).increaseFirstUnusedIndex()
        verify(itemsDataSource).handleModifiedItems(listOf(), listOf(transactionItem))

        Assert.assertNull(result)
    }

    @Test
    fun handleUpdatedRecords_emptyModifiedLists_noNewData() {
        val record1 = mock(TransactionRecord::class.java)
        val records = listOf(record1)

        val pool = mock(Pool::class.java)

        whenever(poolRepo.getPool(walletBtc)).thenReturn(pool)
        whenever(poolRepo.isPoolActiveByWallet(walletBtc)).thenReturn(true)
        whenever(pool.handleUpdatedRecord(record1)).thenReturn(Pool.HandleResult.IGNORED)

        Assert.assertNull(dataSource.handleUpdatedRecords(records, walletBtc))
    }

    @Test
    fun handleUpdatedRecords() {

        val pool = mock(Pool::class.java)

        val updatedRecord1 = mock(TransactionRecord::class.java)
        val updatedRecord2 = mock(TransactionRecord::class.java)
        val insertedRecord1 = mock(TransactionRecord::class.java)
        val ignoredRecord = mock(TransactionRecord::class.java)
        val records = listOf<TransactionRecord>(updatedRecord1, ignoredRecord, updatedRecord2, insertedRecord1)

        val updatedItem1 = TransactionItem(walletBtc, updatedRecord1)
        val updatedItem2 = TransactionItem(walletBtc, updatedRecord2)
        val insertedItem1 = TransactionItem(walletBtc, insertedRecord1)

        whenever(poolRepo.getPool(walletBtc)).thenReturn(pool)
        whenever(poolRepo.isPoolActiveByWallet(walletBtc)).thenReturn(true)
        whenever(pool.handleUpdatedRecord(updatedRecord1)).thenReturn(Pool.HandleResult.UPDATED)
        whenever(pool.handleUpdatedRecord(ignoredRecord)).thenReturn(Pool.HandleResult.IGNORED)
        whenever(pool.handleUpdatedRecord(updatedRecord2)).thenReturn(Pool.HandleResult.UPDATED)
        whenever(pool.handleUpdatedRecord(insertedRecord1)).thenReturn(Pool.HandleResult.INSERTED)
        whenever(factory.createTransactionItem(walletBtc, updatedRecord1)).thenReturn(updatedItem1)
        whenever(factory.createTransactionItem(walletBtc, updatedRecord2)).thenReturn(updatedItem2)
        whenever(factory.createTransactionItem(walletBtc, insertedRecord1)).thenReturn(insertedItem1)

        dataSource.handleUpdatedRecords(records, walletBtc)

        verify(itemsDataSource).handleModifiedItems(listOf(updatedItem1, updatedItem2), listOf(insertedItem1))
    }

    @Test
    fun itemIndexesForPending() {
        val lastBlockHeight = 100

        dataSource.itemIndexesForPending(walletBtc, lastBlockHeight)

        verify(itemsDataSource).itemIndexesForPending(walletBtc, lastBlockHeight)
    }
}
