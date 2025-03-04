package io.horizontalsystems.solanakit.database.main.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.solanakit.models.LastBlockHeightEntity

@Dao
interface LastBlockHeightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(lastBlockHeight: LastBlockHeightEntity)

    @Query("SELECT * FROM LastBlockHeightEntity")
    fun getLastBlockHeight(): LastBlockHeightEntity?

}
