package cash.p.terminal.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cash.p.terminal.entities.PendingMultiSwap
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface PendingMultiSwapDao {
    @Query("SELECT * FROM PendingMultiSwap WHERE accountId = :accountId OR accountId = '' ORDER BY createdAt DESC")
    fun getByAccountId(accountId: String): Flow<List<PendingMultiSwap>>

    @Query("SELECT * FROM PendingMultiSwap WHERE accountId = :accountId OR accountId = '' ORDER BY createdAt DESC")
    suspend fun getAllOnceByAccountId(accountId: String): List<PendingMultiSwap>

    @Query("SELECT * FROM PendingMultiSwap WHERE id = :id")
    suspend fun getById(id: String): PendingMultiSwap?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(swap: PendingMultiSwap)

    @Query("UPDATE PendingMultiSwap SET leg1Status = :status, leg1AmountOut = :amountOut, leg1TransactionId = :transactionId WHERE id = :id")
    suspend fun updateLeg1(id: String, status: String, amountOut: BigDecimal?, transactionId: String?)

    @Query("UPDATE PendingMultiSwap SET leg2Status = :status, leg2AmountOut = :amountOut, leg2TransactionId = :transactionId WHERE id = :id")
    suspend fun updateLeg2(id: String, status: String, amountOut: BigDecimal?, transactionId: String?)

    @Query("UPDATE PendingMultiSwap SET leg1ProviderTransactionId = :providerTransactionId WHERE id = :id")
    suspend fun setLeg1ProviderTransactionId(id: String, providerTransactionId: String)

    @Query("UPDATE PendingMultiSwap SET leg1InfoRecordUid = :recordUid WHERE id = :id AND leg1InfoRecordUid IS NULL")
    suspend fun setLeg1InfoRecordUid(id: String, recordUid: String)

    @Query("DELETE FROM PendingMultiSwap WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM PendingMultiSwap WHERE createdAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT COUNT(*) FROM PendingMultiSwap")
    fun count(): Flow<Int>
}
