package io.horizontalsystems.marketkit.storage

import androidx.room.*
import io.horizontalsystems.marketkit.models.*

@Dao
interface BlockchainEntityDao {

    @Query("SELECT * FROM BlockchainEntity")
    fun getAll(): List<BlockchainEntity>

}
