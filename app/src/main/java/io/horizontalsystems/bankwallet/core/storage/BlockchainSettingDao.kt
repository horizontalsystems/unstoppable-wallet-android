package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.BlockchainSetting
import io.horizontalsystems.coinkit.models.CoinType

@Dao
interface BlockchainSettingDao {

    @Query("SELECT * FROM BlockchainSetting")
    fun getAll(): List<BlockchainSetting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: BlockchainSetting)

    @Query("SELECT * FROM BlockchainSetting WHERE coinType = :coinType AND `key` == :key")
    fun getSetting(coinType: CoinType, key: String): BlockchainSetting?

    @Query("DELETE FROM BlockchainSetting WHERE `key` = :key")
    fun deleteDerivationSettings(key: String)

}
