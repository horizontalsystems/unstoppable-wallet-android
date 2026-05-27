package cash.p.terminal.core.storage

import cash.p.terminal.entities.LocallyCreatedTransactionRecord

class LocallyCreatedTransactionStorage(
    private val dao: LocallyCreatedTransactionDao,
) {
    suspend fun exists(
        accountId: String,
        blockchainTypeUid: String,
        transactionHash: String,
    ): Boolean = dao.exists(accountId, blockchainTypeUid, transactionHash)

    suspend fun insert(record: LocallyCreatedTransactionRecord): Boolean = dao.insert(record) != -1L

    suspend fun trimAccount(accountId: String, limit: Int) {
        dao.trimAccount(accountId, limit)
    }

    suspend fun trimAllAccounts(limit: Int) {
        dao.getAccountIds().forEach { accountId ->
            dao.trimAccount(accountId, limit)
        }
    }

    suspend fun deleteByAccountIds(accountIds: List<String>) {
        if (accountIds.isEmpty()) return
        dao.deleteByAccountIds(accountIds)
    }

    suspend fun count(accountId: String): Int = dao.count(accountId)
}
