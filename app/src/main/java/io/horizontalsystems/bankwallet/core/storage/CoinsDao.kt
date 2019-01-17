package io.horizontalsystems.bankwallet.core.storage

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.horizontalsystems.bankwallet.entities.Coin
import io.reactivex.Flowable

@Dao
interface CoinsDao {

    @Query("SELECT * FROM Coin WHERE code = :coinCode")
    fun getCoin(coinCode: String): Flowable<Coin>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(coin: Coin)

    @Query("SELECT * FROM Coin")
    fun getAll(): Flowable<List<Coin>>

    @Query("DELETE FROM Coin")
    fun deleteAll()

}
