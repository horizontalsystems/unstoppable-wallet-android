package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.entities.CoinType

@Dao
interface MarketFavoritesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(favoriteCoin: FavoriteCoin)

    @Query("DELETE FROM FavoriteCoin WHERE code = :coinCode AND coinType = :coinType")
    fun delete(coinCode: String, coinType: CoinType?)

    @Query("SELECT * FROM FavoriteCoin")
    fun getAll(): List<FavoriteCoin>

    @Query("SELECT COUNT(*) FROM FavoriteCoin WHERE code = :coinCode AND coinType = :coinType")
    fun getCount(coinCode: String, coinType: CoinType?): Int

}

@Entity
data class FavoriteCoin(val code: String, val coinType: CoinType?) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}
