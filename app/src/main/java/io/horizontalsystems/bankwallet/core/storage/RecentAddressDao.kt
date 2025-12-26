package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.RecentAddress
import io.horizontalsystems.marketkit.models.BlockchainType

@Dao
interface RecentAddressDao {

    @Query("SELECT * FROM RecentAddress WHERE accountId = :accountId AND blockchainType = :blockchainType")
    fun get(accountId: String, blockchainType: BlockchainType): RecentAddress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(recentAddress: RecentAddress)

}
