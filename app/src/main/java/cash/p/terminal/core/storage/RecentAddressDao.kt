package cash.p.terminal.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cash.p.terminal.entities.RecentAddress
import io.horizontalsystems.core.entities.BlockchainType

@Dao
interface RecentAddressDao {

    @Query("SELECT * FROM RecentAddress WHERE accountId = :accountId AND blockchainType = :blockchainType")
    fun get(accountId: String, blockchainType: BlockchainType): RecentAddress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(recentAddress: RecentAddress)

}
