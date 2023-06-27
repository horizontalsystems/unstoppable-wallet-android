package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.core.providers.CexAssetRaw

@Dao
interface CexAssetsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(cexAssetRaws: List<CexAssetRaw>)

    @Query("DELETE FROM CexAssetRaw WHERE accountId = :accountId")
    fun delete(accountId: String)

    @Query("SELECT * FROM CexAssetRaw WHERE accountId = :accountId AND id = :id")
    fun get(accountId: String, id: String): CexAssetRaw?

    @Query("SELECT * FROM CexAssetRaw WHERE accountId = :accountId")
    fun getAllForAccount(accountId: String): List<CexAssetRaw>

}
