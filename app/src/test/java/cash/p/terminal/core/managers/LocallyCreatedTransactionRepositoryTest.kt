package cash.p.terminal.core.managers

import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.storage.LocallyCreatedTransactionDao
import cash.p.terminal.core.storage.LocallyCreatedTransactionStorage
import cash.p.terminal.entities.LocallyCreatedTransactionRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocallyCreatedTransactionRepositoryTest {

    @Test
    fun markCreated_validHash_exists() = runTest {
        val repository = createRepository()

        repository.markCreated("account-1", "ethereum", " 0xhash ")

        assertTrue(repository.isCreated("account-1", "ethereum", "0xhash"))
    }

    @Test
    fun isCreated_sameHashDifferentBlockchain_returnsFalse() = runTest {
        val repository = createRepository()

        repository.markCreated("account-1", "ethereum", "same-hash")

        assertFalse(repository.isCreated("account-1", "polygon", "same-hash"))
    }

    @Test
    fun isCreated_sameHashDifferentAccount_returnsFalse() = runTest {
        val repository = createRepository()

        repository.markCreated("account-1", "ethereum", "same-hash")

        assertFalse(repository.isCreated("account-2", "ethereum", "same-hash"))
    }

    @Test
    fun isCreated_storageFailure_returnsFalse() = runTest {
        val dao = FakeLocallyCreatedTransactionDao()
        val repository = createRepository(dao)
        dao.existsFails = true

        assertFalse(repository.isCreated("account-1", "ethereum", "hash"))
    }

    @Test
    fun isCreated_parentCancelled_propagatesCancellation() = runTest {
        val repository = createRepository()
        var completed = false

        val job = launch {
            coroutineContext[Job]?.cancel()
            repository.isCreated("account-1", "ethereum", "hash")
            completed = true
        }
        advanceUntilIdle()

        assertTrue(job.isCancelled)
        assertFalse(completed)
    }

    @Test
    fun markCreated_blankHash_ignored() = runTest {
        val dao = FakeLocallyCreatedTransactionDao()
        val repository = createRepository(dao)

        repository.markCreated("account-1", "ethereum", " ")

        assertEquals(0, dao.records.size)
    }

    @Test
    fun markCreated_storageFailure_doesNotThrow() = runTest {
        val dao = FakeLocallyCreatedTransactionDao()
        val repository = createRepository(dao)
        dao.insertFails = true

        repository.markCreated("account-1", "ethereum", "hash")

        assertEquals(0, dao.records.size)
    }

    @Test
    fun markCreated_parentCancelled_stillPersists() = runTest {
        val repository = createRepository()

        launch {
            coroutineContext[Job]?.cancel()
            repository.markCreated("account-1", "ethereum", "hash")
        }
        advanceUntilIdle()

        assertTrue(repository.isCreated("account-1", "ethereum", "hash"))
    }

    @Test
    fun markCreated_duplicateHash_keepsSingleRecord() = runTest {
        val dao = FakeLocallyCreatedTransactionDao()
        val repository = createRepository(dao)

        repository.markCreated("account-1", "ethereum", "hash")
        repository.markCreated("account-1", "ethereum", "hash")

        assertEquals(1, dao.records.size)
    }

    @Test
    fun markCreated_newRecord_emitsChangedFlow() = runTest {
        val repository = createRepository()
        val emissions = mutableListOf<Unit>()
        val job = launch {
            repository.changedFlow.collect {
                emissions += Unit
            }
        }
        advanceUntilIdle()

        repository.markCreated("account-1", "ethereum", "hash")
        advanceUntilIdle()

        assertEquals(1, emissions.size)
        job.cancel()
    }

    @Test
    fun markCreated_underLimit_doesNotTrim() = runTest {
        val dao = FakeLocallyCreatedTransactionDao()
        val repository = createRepository(dao)
        dao.countOverride = LocallyCreatedTransactionRepository.MAX_RECORDS_PER_ACCOUNT

        repository.markCreated("account-1", "ethereum", "hash")

        assertEquals(0, dao.trimCalls)
    }

    @Test
    fun markCreated_overLimit_trimsAccount() = runTest {
        val dao = FakeLocallyCreatedTransactionDao()
        val repository = createRepository(dao)
        dao.countOverride = LocallyCreatedTransactionRepository.MAX_RECORDS_PER_ACCOUNT + 1

        repository.markCreated("account-1", "ethereum", "hash")

        assertEquals(1, dao.trimCalls)
    }

    @Test
    fun trimAllAccounts_overLimit_keepsLatestPerAccount() = runTest {
        val dao = FakeLocallyCreatedTransactionDao()
        val storage = LocallyCreatedTransactionStorage(dao)

        repeat(3) { index ->
            storage.insert(record("account-1", "hash-$index", createdAt = index.toLong()))
            storage.insert(record("account-2", "hash-$index", createdAt = index.toLong()))
        }

        storage.trimAllAccounts(limit = 2)

        assertEquals(listOf("hash-2", "hash-1"), dao.hashes("account-1"))
        assertEquals(listOf("hash-2", "hash-1"), dao.hashes("account-2"))
    }

    @Test
    fun deleteByAccountIds_deletesOnlySelectedAccounts() = runTest {
        val dao = FakeLocallyCreatedTransactionDao()
        val repository = createRepository(dao)

        repository.markCreated("account-1", "ethereum", "hash-1")
        repository.markCreated("account-2", "ethereum", "hash-2")

        repository.deleteByAccountIds(listOf("account-1"))

        assertFalse(repository.isCreated("account-1", "ethereum", "hash-1"))
        assertTrue(repository.isCreated("account-2", "ethereum", "hash-2"))
    }

    private fun TestScope.createRepository(
        dao: FakeLocallyCreatedTransactionDao = FakeLocallyCreatedTransactionDao(),
    ): LocallyCreatedTransactionRepository {
        return LocallyCreatedTransactionRepository(
            storage = LocallyCreatedTransactionStorage(dao),
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler), this),
        )
    }

    private fun record(
        accountId: String,
        transactionHash: String,
        createdAt: Long,
    ): LocallyCreatedTransactionRecord {
        return LocallyCreatedTransactionRecord(
            accountId = accountId,
            blockchainTypeUid = "ethereum",
            transactionHash = transactionHash,
            createdAt = createdAt,
        )
    }

    private class FakeLocallyCreatedTransactionDao : LocallyCreatedTransactionDao {
        val records = mutableListOf<LocallyCreatedTransactionRecord>()
        var insertFails = false
        var existsFails = false
        var countOverride: Int? = null
        var trimCalls = 0

        override suspend fun insert(record: LocallyCreatedTransactionRecord): Long {
            if (insertFails) error("insert failed")
            if (exists(record.accountId, record.blockchainTypeUid, record.transactionHash)) return -1
            records += record
            return records.size.toLong()
        }

        override suspend fun exists(
            accountId: String,
            blockchainTypeUid: String,
            transactionHash: String,
        ): Boolean {
            if (existsFails) error("exists failed")
            return records.any {
                it.accountId == accountId &&
                    it.blockchainTypeUid == blockchainTypeUid &&
                    it.transactionHash == transactionHash
            }
        }

        override suspend fun getAccountIds(): List<String> {
            return records.map { it.accountId }.distinct()
        }

        override suspend fun trimAccount(accountId: String, limit: Int) {
            trimCalls++
            val keep = records
                .filter { it.accountId == accountId }
                .sortedByDescending { it.createdAt }
                .take(limit)
                .toSet()

            records.removeAll { it.accountId == accountId && it !in keep }
        }

        override suspend fun deleteByAccountIds(accountIds: List<String>) {
            records.removeAll { it.accountId in accountIds }
        }

        override suspend fun count(accountId: String): Int {
            countOverride?.let { return it }
            return records.count { it.accountId == accountId }
        }

        fun hashes(accountId: String): List<String> {
            return records
                .filter { it.accountId == accountId }
                .sortedByDescending { it.createdAt }
                .map { it.transactionHash }
        }
    }
}
