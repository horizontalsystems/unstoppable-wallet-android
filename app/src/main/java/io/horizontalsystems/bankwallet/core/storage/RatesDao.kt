package io.horizontalsystems.bankwallet.core.storage

import android.arch.persistence.room.*
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable

@Dao
interface RatesDao {

    @Query("SELECT * FROM Rate WHERE coinCode = :coinCode AND currencyCode = :currencyCode AND isLatest = 1")
    fun getLatestRate(coinCode: CoinCode, currencyCode: String): Flowable<Rate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(rate: Rate)

    @Delete
    fun delete(rate: Rate)

    @Query("SELECT * FROM Rate")
    fun getAll(): Flowable<List<Rate>>

    @Query("DELETE FROM Rate")
    fun deleteAll()

    @Query("SELECT * FROM Rate WHERE coinCode = :coinCode AND currencyCode = :currencyCode AND timestamp = :timestamp")
    fun getRate(coinCode: CoinCode, currencyCode: String, timestamp: Long): Flowable<List<Rate>>

    @Query("SELECT * FROM Rate WHERE value = 0.0")
    fun getZeroRates(): Flowable<List<Rate>>

}
