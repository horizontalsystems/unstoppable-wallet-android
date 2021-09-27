package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.CustomToken
import io.horizontalsystems.marketkit.models.CoinType

@Dao
interface CustomTokenDao {
    @Query("SELECT * FROM CustomToken")
    fun getCustomTokens(): List<CustomToken>

    @Query(
        "SELECT * FROM CustomToken " +
                "WHERE coinName LIKE :filter OR coinCode LIKE :filter " +
                "ORDER BY coinName"
    )
    fun getCustomTokens(filter: String): List<CustomToken>

    @Query("SELECT * FROM CustomToken WHERE coinType in (:coinTypeIds)")
    fun getCustomTokens(coinTypeIds: List<String>): List<CustomToken>

    @Query("SELECT * FROM CustomToken WHERE coinType = :coinType")
    fun getCustomToken(coinType: CoinType): CustomToken?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(customTokens: List<CustomToken>)
}
