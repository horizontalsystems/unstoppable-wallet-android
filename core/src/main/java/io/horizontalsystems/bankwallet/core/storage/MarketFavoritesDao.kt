package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*

@Dao
interface MarketFavoritesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(favoriteCoin: FavoriteCoin)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(favoriteCoins: List<FavoriteCoin>)

    @Query("DELETE FROM FavoriteCoin WHERE coinUid = :coinUid")
    fun delete(coinUid: String)

    @Query("SELECT * FROM FavoriteCoin")
    fun getAll(): List<FavoriteCoin>

    @Query("SELECT COUNT(*) FROM FavoriteCoin WHERE coinUid = :coinUid")
    fun getCount(coinUid: String): Int

}

@Entity
data class FavoriteCoin(@PrimaryKey val coinUid: String)
