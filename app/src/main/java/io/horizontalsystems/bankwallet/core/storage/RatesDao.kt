package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface RatesDao {

    @Query("SELECT * FROM Rate WHERE coinCode = :coinCode AND currencyCode = :currencyCode AND isLatest = 1")
    fun getLatestRateFlowable(coinCode: CoinCode, currencyCode: String): Flowable<Rate>

    @Query("SELECT * FROM Rate WHERE coinCode = :coinCode AND currencyCode = :currencyCode AND isLatest = 1")
    fun getLatestRate(coinCode: CoinCode, currencyCode: String): Rate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(rate: Rate)

    @Delete
    fun delete(rate: Rate)

    @Query("DELETE FROM Rate WHERE isLatest = 1 AND coinCode = :coinCode AND currencyCode = :currencyCode")
    fun deleteLatest(coinCode: CoinCode, currencyCode: String)

    @Query("DELETE FROM Rate")
    fun deleteAll()

    @Query("SELECT * FROM Rate WHERE coinCode = :coinCode AND currencyCode = :currencyCode AND timestamp = :timestamp AND isLatest = 0")
    fun getRate(coinCode: CoinCode, currencyCode: String, timestamp: Long): Single<Rate>

    @Query("SELECT * FROM Rate WHERE value = 0.0 AND currencyCode = :currencyCode AND isLatest = 0")
    fun getZeroRates(currencyCode: String): Single<List<Rate>>

}
