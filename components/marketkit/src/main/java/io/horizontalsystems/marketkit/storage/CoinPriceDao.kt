package io.horizontalsystems.marketkit.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.marketkit.models.CoinPrice

@Dao
interface CoinPriceDao {
    @Query("SELECT * FROM CoinPrice WHERE coinUid=:coinUid AND currencyCode=:currencyCode")
    fun getCoinPrice(coinUid: String, currencyCode: String): CoinPrice?

    @Query("SELECT * FROM CoinPrice WHERE coinUid IN (:coinUids) AND currencyCode=:currencyCode")
    fun getCoinPrices(coinUids: List<String>, currencyCode: String): List<CoinPrice>

    @Query("SELECT * FROM CoinPrice WHERE coinUid in (:coinUids) AND currencyCode=:currencyCode ORDER BY timestamp")
    fun getCoinPricesSortedByTimestamp(coinUids: List<String>, currencyCode: String): List<CoinPrice>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(coinPrices: List<CoinPrice>)
}
