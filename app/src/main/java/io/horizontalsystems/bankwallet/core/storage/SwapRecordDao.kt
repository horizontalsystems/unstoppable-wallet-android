package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.modules.multiswap.SwapRecord

@Dao
interface SwapRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(record: SwapRecord)

    @Query("SELECT * FROM SwapRecord ORDER BY timestamp DESC")
    fun getAll(): List<SwapRecord>

    @Query("SELECT * FROM SwapRecord WHERE status NOT IN ('Completed', 'Refunded', 'Failed') ORDER BY timestamp DESC")
    fun getPending(): List<SwapRecord>

    @Query("SELECT * FROM SwapRecord WHERE id = :id")
    fun getById(id: Int): SwapRecord?

    @Query("UPDATE SwapRecord SET status = :status WHERE id = :id")
    fun updateStatus(id: Int, status: String)

    @Query("DELETE FROM SwapRecord WHERE id = :id")
    fun delete(id: Int)
}
