package io.horizontalsystems.bankwallet.modules.transactions

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito.mock

class PoolTest {

    private val state = mock(Pool.State::class.java)
    private val pool = Pool(state)
    private val wallet = mock(Wallet::class.java)

    @Test
    fun allShown() {
        whenever(state.allLoaded).thenReturn(true)
        whenever(state.unusedRecords).thenReturn(listOf())

        Assert.assertTrue(pool.allShown)
    }

    @Test
    fun allShown_notAllLoaded() {
        whenever(state.allLoaded).thenReturn(false)
        whenever(state.unusedRecords).thenReturn(listOf(mock(TransactionRecord::class.java)))

        Assert.assertFalse(pool.allShown)
    }

    @Test
    fun allShown_notEmptyRecords() {
        whenever(state.allLoaded).thenReturn(true)
        whenever(state.unusedRecords).thenReturn(listOf(mock(TransactionRecord::class.java)))

        Assert.assertFalse(pool.allShown)
    }

    @Test
    fun unusedRecords() {
        val list = listOf(mock(TransactionRecord::class.java))

        whenever(state.unusedRecords).thenReturn(list)

        Assert.assertArrayEquals(list.toTypedArray(), pool.unusedRecords.toTypedArray())
    }

    @Test
    fun records() {
        val list = mutableListOf(mock(TransactionRecord::class.java))

        whenever(state.records).thenReturn(list)

        Assert.assertArrayEquals(list.toTypedArray(), pool.records.toTypedArray())
    }

    @Test
    fun wallet() {
        whenever(state.wallet).thenReturn(wallet)

        Assert.assertEquals(wallet, pool.wallet)
    }

    @Test
    fun increaseFirstUnusedIndex() {
        whenever(state.firstUnusedIndex).thenReturn(100)

        pool.increaseFirstUnusedIndex()

        verify(state).firstUnusedIndex = 101
    }

    @Test
    fun resetFirstUnusedIndex() {
        pool.resetFirstUnusedIndex()

        verify(state).firstUnusedIndex = 0
    }

    @Test
    fun getFetchData_allLoaded() {
        whenever(state.allLoaded).thenReturn(true)

        Assert.assertNull(pool.getFetchData(10))
    }

    @Test
    fun getFetchData_enoughUnusedRecords() {
        val unusedRecords = mock<List<TransactionRecord>>()

        whenever(state.allLoaded).thenReturn(false)
        whenever(state.unusedRecords).thenReturn(unusedRecords)
        whenever(unusedRecords.size).thenReturn(11)

        Assert.assertNull(pool.getFetchData(10))
    }

    @Test
    fun getFetchData_hasRecords() {
        val limit = 10
        val unusedRecordsSize = 8
        val unusedRecords = mock<List<TransactionRecord>>()
        val lastRecordHash = "hash"
        val lastRecord = mock(TransactionRecord::class.java)

        whenever(unusedRecords.size).thenReturn(unusedRecordsSize)
        whenever(state.allLoaded).thenReturn(false)
        whenever(state.unusedRecords).thenReturn(unusedRecords)
        whenever(state.wallet).thenReturn(wallet)
        whenever(state.records).thenReturn(mutableListOf(mock(TransactionRecord::class.java), lastRecord))
        whenever(lastRecord.transactionHash).thenReturn(lastRecordHash)

        val fetchData = TransactionsModule.FetchData(wallet, lastRecord, limit - unusedRecordsSize + 1)

        Assert.assertEquals(fetchData, pool.getFetchData(limit))
    }

    @Test
    fun getFetchData_noRecords() {
        val limit = 10
        val unusedRecordsSize = 8
        val unusedRecords = mock<List<TransactionRecord>>()

        whenever(unusedRecords.size).thenReturn(unusedRecordsSize)
        whenever(state.allLoaded).thenReturn(false)
        whenever(state.unusedRecords).thenReturn(unusedRecords)
        whenever(state.wallet).thenReturn(wallet)
        whenever(state.records).thenReturn(mutableListOf())

        val fetchData = TransactionsModule.FetchData(wallet, null, limit - unusedRecordsSize + 1)

        Assert.assertEquals(fetchData, pool.getFetchData(limit))
    }

    @Test
    fun add_empty() {
        pool.add(listOf())

        verify(state).allLoaded = true
        verifyNoMoreInteractions(state)
    }

    @Test
    fun add_notEmpty() {
        val records = listOf(mock(TransactionRecord::class.java), mock(TransactionRecord::class.java))

        pool.add(records)

        verify(state).add(records)
        verifyNoMoreInteractions(state)
    }

    @Test
    fun handleUpdatedRecord_noIndexToInsert_allLoaded_noUnusedRecords() {
        val record = mock(TransactionRecord::class.java)

        whenever(state.indexOf(record)).thenReturn(-1)
        whenever(state.insertIndexOf(record)).thenReturn(-1)
        whenever(state.allLoaded).thenReturn(true)
        whenever(state.unusedRecords).thenReturn(listOf())

        val result = pool.handleUpdatedRecord(record)

        verify(state).add(listOf(record))

        Assert.assertEquals(Pool.HandleResult.NEW_DATA, result)
    }

    @Test
    fun handleUpdatedRecord_noIndexToInsert_allLoaded_hasUnusedRecords() {
        val record = mock(TransactionRecord::class.java)

        whenever(state.indexOf(record)).thenReturn(-1)
        whenever(state.insertIndexOf(record)).thenReturn(-1)
        whenever(state.allLoaded).thenReturn(true)
        whenever(state.unusedRecords).thenReturn(listOf(mock(TransactionRecord::class.java)))

        val result = pool.handleUpdatedRecord(record)

        Assert.assertEquals(Pool.HandleResult.IGNORED, result)
    }

    @Test
    fun handleUpdatedRecord_insertedIntoEmpty() {
        val insertIndex = 0
        val record = mock(TransactionRecord::class.java)

        whenever(state.indexOf(record)).thenReturn(-1)
        whenever(state.insertIndexOf(record)).thenReturn(insertIndex)
        whenever(state.firstUnusedIndex).thenReturn(0)

        val result = pool.handleUpdatedRecord(record)

        verify(state).insertRecord(insertIndex, record)
        Assert.assertEquals(Pool.HandleResult.NEW_DATA, result)
    }

    @Test
    fun handleUpdatedRecord_insertedInAlreadyShown() {
        val insertIndex = 144
        val record = mock(TransactionRecord::class.java)

        whenever(state.indexOf(record)).thenReturn(-1)
        whenever(state.insertIndexOf(record)).thenReturn(insertIndex)
        whenever(state.firstUnusedIndex).thenReturn(145)

        val result = pool.handleUpdatedRecord(record)

        verify(state).insertRecord(insertIndex, record)
        verify(state).firstUnusedIndex = 146
        Assert.assertEquals(Pool.HandleResult.INSERTED, result)
    }

    @Test
    fun handleUpdatedRecord_insertedInNotShown() {
        val insertIndex = 145
        val record = mock(TransactionRecord::class.java)

        whenever(state.indexOf(record)).thenReturn(-1)
        whenever(state.insertIndexOf(record)).thenReturn(insertIndex)
        whenever(state.firstUnusedIndex).thenReturn(145)

        val result = pool.handleUpdatedRecord(record)

        verify(state).insertRecord(insertIndex, record)
        verify(state, never()).firstUnusedIndex = 146
        Assert.assertEquals(Pool.HandleResult.IGNORED, result)
    }

    @Test
    fun handleUpdatedRecord_updatedShown() {
        val index = 148
        val record = mock(TransactionRecord::class.java)

        whenever(state.indexOf(record)).thenReturn(index)
        whenever(state.firstUnusedIndex).thenReturn(149)

        val result = pool.handleUpdatedRecord(record)

        verify(state).setRecord(index, record)
        Assert.assertEquals(Pool.HandleResult.UPDATED, result)
    }

    @Test
    fun handleUpdatedRecord_updatedNotShown() {
        val index = 148
        val record = mock(TransactionRecord::class.java)

        whenever(state.indexOf(record)).thenReturn(index)
        whenever(state.firstUnusedIndex).thenReturn(147)

        val result = pool.handleUpdatedRecord(record)

        verify(state).setRecord(index, record)
        Assert.assertEquals(Pool.HandleResult.IGNORED, result)
    }

}
