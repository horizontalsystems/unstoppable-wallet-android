package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.RestoreSettingRecord

@Dao
interface RestoreSettingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(records: List<RestoreSettingRecord>)

    @Query("SELECT * FROM `RestoreSettingRecord` WHERE accountId = :accountId AND coinId = :coinId")
    fun get(accountId: String, coinId: String): List<RestoreSettingRecord>

    @Query("SELECT * FROM `RestoreSettingRecord` WHERE accountId = :accountId")
    fun get(accountId: String): List<RestoreSettingRecord>

    @Query("DELETE FROM `RestoreSettingRecord` WHERE accountId = :accountId")
    fun delete(accountId: String)

}
