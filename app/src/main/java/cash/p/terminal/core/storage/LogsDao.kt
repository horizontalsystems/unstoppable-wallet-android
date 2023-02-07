package cash.p.terminal.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cash.p.terminal.entities.LogEntry

@Dao
interface LogsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(logEntry: LogEntry)

    @Query("SELECT * FROM LogEntry ORDER BY id")
    fun getAll(): List<LogEntry>

}
