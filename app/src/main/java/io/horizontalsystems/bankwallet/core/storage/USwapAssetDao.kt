package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.USwapAssetRecord

@Dao
interface USwapAssetDao {

    @Query("SELECT * FROM USwapAssetRecord WHERE providerId = :providerId")
    fun getByProvider(providerId: String): List<USwapAssetRecord>

    @Query("SELECT MIN(timestamp) FROM USwapAssetRecord WHERE providerId = :providerId")
    fun getOldestTimestamp(providerId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(records: List<USwapAssetRecord>)

    @Query("DELETE FROM USwapAssetRecord WHERE providerId = :providerId")
    fun deleteByProvider(providerId: String)
}
