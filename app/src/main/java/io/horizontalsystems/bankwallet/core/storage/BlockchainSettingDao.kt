package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.entities.BlockchainSetting
import io.horizontalsystems.bankwallet.entities.CoinType

@Dao
interface BlockchainSettingDao {

    @Query("SELECT * FROM BlockchainSetting")
    fun getAll(): List<BlockchainSetting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: BlockchainSetting)

    @Query("DELETE FROM BlockchainSetting")
    fun deleteAll()

    @Query("SELECT * FROM BlockchainSetting WHERE coinType = :coinType")
    fun getSetting(coinType: CoinType): BlockchainSetting?

}
