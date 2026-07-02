package io.horizontalsystems.walletkit.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.walletkit.entities.SyncerState

@Dao
interface SyncerStateDao {

    @Query("SELECT * FROM SyncerState WHERE `key` = :key")
    fun get(key: String): SyncerState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(state: SyncerState)

}
