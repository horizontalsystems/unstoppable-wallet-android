package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.SwapProviderAssetRecord

@Dao
interface SwapProviderAssetDao {

    @Query("SELECT * FROM SwapProviderAssetRecord WHERE providerId = :providerId")
    fun getByProvider(providerId: String): List<SwapProviderAssetRecord>

    @Query("SELECT MIN(timestamp) FROM SwapProviderAssetRecord WHERE providerId = :providerId")
    fun getOldestTimestamp(providerId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(records: List<SwapProviderAssetRecord>)

    @Query("DELETE FROM SwapProviderAssetRecord WHERE providerId = :providerId")
    fun deleteByProvider(providerId: String)
}
