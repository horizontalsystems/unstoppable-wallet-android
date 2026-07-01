package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.BlockchainSettingRecord

@Dao
interface BlockchainSettingDao {

    @Query("SELECT * FROM BlockchainSettingRecord")
    fun getAll(): List<BlockchainSettingRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: BlockchainSettingRecord)

    @Query("SELECT * FROM BlockchainSettingRecord WHERE blockchainUid = :blockchainUid AND `key` == :key")
    fun getBlockchainSetting(blockchainUid: String, key: String): BlockchainSettingRecord?

}
