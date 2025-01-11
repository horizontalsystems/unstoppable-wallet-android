package cash.p.terminal.wallet.storage

import androidx.room.*
import cash.p.terminal.wallet.models.BlockchainEntity

@Dao
interface BlockchainEntityDao {

    @Query("SELECT * FROM BlockchainEntity")
    fun getAll(): List<BlockchainEntity>

}
