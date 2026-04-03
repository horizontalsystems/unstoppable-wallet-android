package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.SwapProviderChainRecord

@Dao
interface SwapProviderChainDao {

    @Query("SELECT * FROM SwapProviderChainRecord WHERE providerId = :providerId")
    fun getByProvider(providerId: String): List<SwapProviderChainRecord>

    @Query("SELECT MIN(timestamp) FROM SwapProviderChainRecord WHERE providerId = :providerId")
    fun getOldestTimestamp(providerId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(records: List<SwapProviderChainRecord>)

    @Query("DELETE FROM SwapProviderChainRecord WHERE providerId = :providerId")
    fun deleteByProvider(providerId: String)
}
