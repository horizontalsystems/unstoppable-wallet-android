package io.horizontalsystems.bankwallet.core.storage

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.horizontalsystems.bankwallet.entities.StorableCoin
import io.reactivex.Flowable

@Dao
interface StorableCoinsDao {

    @Query("SELECT * FROM StorableCoin WHERE coinCode = :coinCode")
    fun getCoin(coinCode: String): Flowable<StorableCoin>

    @Query("SELECT * FROM StorableCoin ORDER BY `coinTitle` ASC")
    fun getAllCoins(): Flowable<List<StorableCoin>>

    @Query("SELECT * FROM StorableCoin WHERE enabled = 1 ORDER BY `order` ASC")
    fun getEnabledCoin(): Flowable<List<StorableCoin>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(storableCoin: StorableCoin)

    @Query("DELETE FROM StorableCoin")
    fun deleteAll()

    @Query("UPDATE StorableCoin SET enabled = 0, `order` = null")
    fun resetCoinsState()

}
