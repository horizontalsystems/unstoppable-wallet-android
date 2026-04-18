package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.SwapRecord

@Dao
interface SwapRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(record: SwapRecord)

    @Query("SELECT * FROM SwapRecord WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getAll(accountId: String): List<SwapRecord>

    @Query("SELECT * FROM SwapRecord WHERE accountId = :accountId AND status NOT IN ('Completed', 'Refunded', 'Failed') ORDER BY timestamp DESC")
    fun getPending(accountId: String): List<SwapRecord>

    @Query("SELECT * FROM SwapRecord WHERE id = :id")
    fun getById(id: Int): SwapRecord?

    @Query("UPDATE SwapRecord SET status = :status WHERE id = :id")
    fun updateStatus(id: Int, status: String)

    @Query("UPDATE SwapRecord SET status = :status, amountOut = :amountOut WHERE id = :id")
    fun updateStatusAndAmountOut(id: Int, status: String, amountOut: String)

    @Query("UPDATE SwapRecord SET transactionHash = :hash WHERE id = :id")
    fun updateTransactionHash(id: Int, hash: String)

    @Query("UPDATE SwapRecord SET outboundTransactionHash = :hash WHERE id = :id")
    fun updateOutboundTransactionHash(id: Int, hash: String)

    @Query("DELETE FROM SwapRecord WHERE id = :id")
    fun delete(id: Int)
}
