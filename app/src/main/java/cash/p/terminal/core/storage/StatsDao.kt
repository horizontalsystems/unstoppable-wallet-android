package cash.p.terminal.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cash.p.terminal.entities.StatRecord

@Dao
interface StatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(statRecord: StatRecord)

    @Query("SELECT * FROM StatRecord ORDER BY id")
    fun getAll(): List<StatRecord>

    @Query("DELETE FROM StatRecord WHERE id IN (:ids)")
    fun delete(ids: List<Int>): Int

}
