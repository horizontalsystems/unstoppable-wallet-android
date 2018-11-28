package io.horizontalsystems.bankwallet.core.storage

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.Coin
import io.reactivex.Single

@Dao
interface RatesDao {

    @Query("SELECT * FROM Rate WHERE coin = :coin AND currencyCode = :currencyCode")
    fun getRate(coin: Coin, currencyCode: String): Single<Rate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(rate: Rate)

    @Query("DELETE FROM Rate")
    fun deleteAll()

}
