package io.horizontalsystems.solanakit.database.main.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.solanakit.models.InitialSyncEntity

@Dao
interface InitialSyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: InitialSyncEntity)

    @Query("SELECT * FROM InitialSyncEntity")
    fun getAllEntities(): List<InitialSyncEntity>
}