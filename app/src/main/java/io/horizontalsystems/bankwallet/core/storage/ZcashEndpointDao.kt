package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.ZcashEndpointRecord

@Dao
interface ZcashEndpointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: ZcashEndpointRecord)

    @Query("SELECT * FROM ZcashEndpointRecord")
    fun getAll(): List<ZcashEndpointRecord>

    @Query("DELETE FROM ZcashEndpointRecord WHERE url = :url")
    fun delete(url: String)

}
