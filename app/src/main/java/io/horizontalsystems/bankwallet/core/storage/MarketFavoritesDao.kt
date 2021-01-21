package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.entities.CoinType

@Dao
interface MarketFavoritesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(favoriteCoin: FavoriteCoin)

    @Query("DELETE FROM FavoriteCoin WHERE code = :coinCode")
    fun delete(coinCode: String)

    @Query("SELECT * FROM FavoriteCoin")
    fun getAll(): List<FavoriteCoin>

    @Query("SELECT COUNT(*) FROM FavoriteCoin WHERE code = :coinCode")
    fun getCount(coinCode: String): Int

}

@Entity
data class FavoriteCoin(val code: String) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}
