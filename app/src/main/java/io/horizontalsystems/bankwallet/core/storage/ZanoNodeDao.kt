package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.ZanoNodeRecord

@Dao
interface ZanoNodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: ZanoNodeRecord)

    @Query("SELECT * FROM ZanoNodeRecord")
    fun getAll(): List<ZanoNodeRecord>

    @Query("DELETE FROM ZanoNodeRecord WHERE url = :url")
    fun delete(url: String)

}
