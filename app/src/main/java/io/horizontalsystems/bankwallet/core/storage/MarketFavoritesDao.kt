package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.coinkit.models.CoinType

@Dao
interface MarketFavoritesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(favoriteCoin: FavoriteCoin)

    @Query("DELETE FROM FavoriteCoin WHERE coinType = :coinType")
    fun delete(coinType: CoinType)

    @Query("SELECT * FROM FavoriteCoin")
    fun getAll(): List<FavoriteCoin>

    @Query("SELECT COUNT(*) FROM FavoriteCoin WHERE coinType = :coinType")
    fun getCount(coinType: CoinType): Int

}

@Entity
data class FavoriteCoin(val coinType: CoinType) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}
