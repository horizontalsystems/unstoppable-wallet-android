package io.horizontalsystems.stellarkit.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OperationDao {

    @Query("SELECT * FROM Event WHERE id < :beforeId ORDER BY id DESC LIMIT 0, :limit")
    fun operations(beforeId: Long, limit: Int): List<Event>

    @Query("SELECT * FROM Event ORDER BY id DESC LIMIT 0, 1")
    fun latestEvent(): Event?

    @Query("SELECT * FROM EventSyncState LIMIT 0, 1")
    fun eventSyncState(): EventSyncState?

    @Query("SELECT * FROM Event ORDER BY id ASC LIMIT 0, 1")
    fun oldestEvent(): Event?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(eventSyncState: EventSyncState)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(events: List<Event>)

    fun resave(tags: List<Tag>, eventIds: List<Long>) {
        deleteTags(eventIds)
        insertTags(tags)
    }

    @Query("DELETE FROM Tag WHERE eventId IN (:eventIds)")
    fun deleteTags(eventIds: List<Long>)

    @Insert
    fun insertTags(tags: List<Tag>)
}