package cash.p.terminal.wallet.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cash.p.terminal.wallet.models.SyncerState

@Dao
interface SyncerStateDao {
    fun save(key: String, value: String) {
        val syncerState = SyncerState(key, value)
        insert(syncerState)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(syncerState: SyncerState)

    @Query("SELECT `value` FROM SyncerState WHERE `key` = :key")
    fun get(key: String) : String?

}
