package cash.p.terminal.wallet.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cash.p.terminal.wallet.models.CoinHistoricalPrice

@Dao
interface CoinHistoricalPriceDao {
    @Query("SELECT * FROM CoinHistoricalPrice WHERE coinUid=:coinUid AND currencyCode=:currencyCode AND timestamp = :timestamp LIMIT 1")
    fun getCoinHistoricalPrice(coinUid: String, currencyCode: String, timestamp: Long): CoinHistoricalPrice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(coinHistoricalPrice: CoinHistoricalPrice)
}
