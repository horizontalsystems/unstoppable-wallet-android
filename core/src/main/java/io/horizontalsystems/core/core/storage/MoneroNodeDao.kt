package io.horizontalsystems.core.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.core.entities.MoneroNodeRecord

@Dao
interface MoneroNodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: MoneroNodeRecord)

    @Query("SELECT * FROM MoneroNodeRecord")
    fun getAll(): List<MoneroNodeRecord>

    @Query("DELETE FROM MONERONODERECORD WHERE url = :url")
    fun delete(url: String)

}
