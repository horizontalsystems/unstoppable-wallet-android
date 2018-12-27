package io.horizontalsystems.bankwallet.core.storage

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import io.reactivex.Maybe

@Dao
interface RatesDao {

    @Query("SELECT * FROM Rate WHERE coinCode = :coinCode AND currencyCode = :currencyCode")
    fun getRateX(coinCode: CoinCode, currencyCode: String): Flowable<Rate>

    @Query("SELECT * FROM Rate WHERE coinCode = :coinCode AND currencyCode = :currencyCode")
    fun getRate(coinCode: CoinCode, currencyCode: String): Maybe<Rate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(rate: Rate)

    @Query("SELECT * FROM Rate")
    fun getAll(): Flowable<List<Rate>>

    @Query("DELETE FROM Rate")
    fun deleteAll()

}
