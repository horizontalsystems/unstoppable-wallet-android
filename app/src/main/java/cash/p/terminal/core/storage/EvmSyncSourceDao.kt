package cash.p.terminal.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cash.p.terminal.entities.EvmSyncSourceRecord

@Dao
interface EvmSyncSourceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: EvmSyncSourceRecord)

    @Query("SELECT * FROM EvmSyncSourceRecord WHERE blockchainTypeUid = :blockchainTypeUid")
    fun getEvmSyncSources(blockchainTypeUid: String): List<EvmSyncSourceRecord>

    @Query("DELETE FROM EvmSyncSourceRecord WHERE blockchainTypeUid = :blockchainTypeUid AND url = :url")
    fun delete(blockchainTypeUid: String, url: String)

}
