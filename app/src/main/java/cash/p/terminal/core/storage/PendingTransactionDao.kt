package cash.p.terminal.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cash.p.terminal.entities.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: PendingTransactionEntity)

    @Query("UPDATE PendingTransaction SET txHash = :txId WHERE id = :id")
    suspend fun updateTxId(id: String, txId: String)

    @Query("SELECT * FROM PendingTransaction WHERE id = :id")
    suspend fun getById(id: String): PendingTransactionEntity?

    @Query(
        """
        SELECT * FROM PendingTransaction
        WHERE expiresAt > :now
          AND walletId = :walletId
          AND balanceConfirmedAt IS NULL
        """
    )
    fun getActivePendingFlow(now: Long, walletId: String): Flow<List<PendingTransactionEntity>>

    @Query("SELECT * FROM PendingTransaction WHERE walletId = :walletId AND expiresAt > :now")
    suspend fun getPendingForWallet(walletId: String, now: Long): List<PendingTransactionEntity>

    @Query("UPDATE PendingTransaction SET balanceConfirmedAt = :confirmedAt WHERE id IN (:ids)")
    suspend fun markBalanceConfirmed(ids: List<String>, confirmedAt: Long)

    @Query("SELECT * FROM PendingTransaction")
    suspend fun getAllPending(): List<PendingTransactionEntity>

    @Query("DELETE FROM PendingTransaction WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM PendingTransaction WHERE expiresAt < :now")
    suspend fun getExpired(now: Long): List<PendingTransactionEntity>

    @Query("DELETE FROM PendingTransaction WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
