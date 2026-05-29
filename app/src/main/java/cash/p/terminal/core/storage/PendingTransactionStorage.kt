package cash.p.terminal.core.storage

import cash.p.terminal.entities.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow

class PendingTransactionStorage(appDatabase: AppDatabase) {
    private val dao = appDatabase.pendingTransactionDao()

    suspend fun insert(entity: PendingTransactionEntity) = dao.insert(entity)

    suspend fun updateTxId(id: String, txId: String) = dao.updateTxId(id, txId)

    suspend fun getById(id: String): PendingTransactionEntity? = dao.getById(id)

    fun getActivePendingFlow(walletId: String): Flow<List<PendingTransactionEntity>> =
        dao.getActivePendingFlow(System.currentTimeMillis(), walletId)

    suspend fun getPendingForWallet(walletId: String): List<PendingTransactionEntity> =
        dao.getPendingForWallet(walletId, System.currentTimeMillis())

    suspend fun markBalanceConfirmed(ids: List<String>, confirmedAt: Long) {
        if (ids.isEmpty()) return
        dao.markBalanceConfirmed(ids, confirmedAt)
    }

    suspend fun deleteById(id: String) = dao.deleteById(id)

    suspend fun getExpired(): List<PendingTransactionEntity> =
        dao.getExpired(System.currentTimeMillis())

    suspend fun hasPendingTransactions(): Boolean {
        return dao.getAllPending().isNotEmpty()
    }

    suspend fun deleteByIds(ids: List<String>) {
        if (ids.isEmpty()) return
        dao.deleteByIds(ids)
    }
}
