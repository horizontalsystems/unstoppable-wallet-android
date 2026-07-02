package io.horizontalsystems.core.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.core.entities.ZcashEndpointRecord

@Dao
interface ZcashEndpointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: ZcashEndpointRecord)

    @Query("SELECT * FROM ZcashEndpointRecord")
    fun getAll(): List<ZcashEndpointRecord>

    @Query("DELETE FROM ZcashEndpointRecord WHERE url = :url")
    fun delete(url: String)

}
