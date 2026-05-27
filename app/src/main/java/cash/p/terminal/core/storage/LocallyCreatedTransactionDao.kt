package cash.p.terminal.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cash.p.terminal.entities.LocallyCreatedTransactionRecord

@Dao
interface LocallyCreatedTransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: LocallyCreatedTransactionRecord): Long

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM LocallyCreatedTransaction
            WHERE accountId = :accountId
              AND blockchainTypeUid = :blockchainTypeUid
              AND transactionHash = :transactionHash
        )
        """
    )
    suspend fun exists(
        accountId: String,
        blockchainTypeUid: String,
        transactionHash: String,
    ): Boolean

    @Query("SELECT DISTINCT accountId FROM LocallyCreatedTransaction")
    suspend fun getAccountIds(): List<String>

    @Query(
        """
        DELETE FROM LocallyCreatedTransaction
        WHERE accountId = :accountId
          AND rowid NOT IN (
              SELECT rowid FROM LocallyCreatedTransaction
              WHERE accountId = :accountId
              ORDER BY createdAt DESC, rowid DESC
              LIMIT :limit
          )
        """
    )
    suspend fun trimAccount(accountId: String, limit: Int)

    @Query("DELETE FROM LocallyCreatedTransaction WHERE accountId IN (:accountIds)")
    suspend fun deleteByAccountIds(accountIds: List<String>)

    @Query("SELECT COUNT(*) FROM LocallyCreatedTransaction WHERE accountId = :accountId")
    suspend fun count(accountId: String): Int
}
