package com.quantum.wallet.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quantum.wallet.bankwallet.entities.SyncerState

@Dao
interface SyncerStateDao {

    @Query("SELECT * FROM SyncerState WHERE `key` = :key")
    fun get(key: String): SyncerState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(state: SyncerState)

}
