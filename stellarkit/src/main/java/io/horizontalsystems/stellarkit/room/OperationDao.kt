package io.horizontalsystems.stellarkit.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import io.horizontalsystems.stellarkit.TagQuery

@Dao
interface OperationDao {

    fun operations(tagQuery: TagQuery, beforeId: Long?, limit: Int): List<Event> {
        val arguments = mutableListOf<String>()
        val whereConditions = mutableListOf<String>()
        var joinClause = ""

        if (!tagQuery.isEmpty) {
            tagQuery.type?.let { type ->
                whereConditions.add("Tag.type = ?")
                arguments.add(type.name)
            }
            tagQuery.assetId?.let { assetId ->
                whereConditions.add("Tag.assetId = ?")
                arguments.add(assetId)
            }
            tagQuery.accountId?.let { accountId ->
                whereConditions.add("Tag.accountIds LIKE ?")
                arguments.add("%${accountId}%")
            }

            joinClause = "INNER JOIN tag ON event.id = tag.eventId"
        }

        beforeId?.let {
            whereConditions.add("event.id < ?")
            arguments.add(it.toString())
        }

        val limitClause = "LIMIT $limit"
        val orderClause = "ORDER BY event.id DESC"
        val whereClause = if (whereConditions.size > 0) {
            "WHERE ${whereConditions.joinToString(" AND ")}"
        } else {
            ""
        }

        val sql = """
            SELECT DISTINCT Event.*
            FROM Event
            $joinClause
            $whereClause
            $orderClause
            $limitClause
            """

        val query = SimpleSQLiteQuery(sql, arguments.toTypedArray())

        return events(query)
    }

    @RawQuery
    fun events(query: SupportSQLiteQuery): List<Event>

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