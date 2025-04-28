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

    fun operations(tagQuery: TagQuery, beforeId: Long?, limit: Int): List<Operation> {
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

            joinClause = "INNER JOIN tag ON operation.id = tag.operationId"
        }

        beforeId?.let {
            whereConditions.add("operation.id < ?")
            arguments.add(it.toString())
        }

        val limitClause = "LIMIT $limit"
        val orderClause = "ORDER BY operation.id DESC"
        val whereClause = if (whereConditions.size > 0) {
            "WHERE ${whereConditions.joinToString(" AND ")}"
        } else {
            ""
        }

        val sql = """
            SELECT DISTINCT Operation.*
            FROM Operation
            $joinClause
            $whereClause
            $orderClause
            $limitClause
            """

        val query = SimpleSQLiteQuery(sql, arguments.toTypedArray())

        return operations(query)
    }

    @RawQuery
    fun operations(query: SupportSQLiteQuery): List<Operation>

    @Query("SELECT * FROM Operation ORDER BY id DESC LIMIT 0, 1")
    fun latestOperation(): Operation?

    @Query("SELECT * FROM OperationSyncState LIMIT 0, 1")
    fun operationSyncState(): OperationSyncState?

    @Query("SELECT * FROM Operation ORDER BY id ASC LIMIT 0, 1")
    fun oldestOperation(): Operation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(operationSyncState: OperationSyncState)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(operations: List<Operation>)

    fun resave(tags: List<Tag>, operationIds: List<Long>) {
        deleteTags(operationIds)
        insertTags(tags)
    }

    @Query("DELETE FROM Tag WHERE operationId IN (:operationIds)")
    fun deleteTags(operationIds: List<Long>)

    @Insert
    fun insertTags(tags: List<Tag>)
}