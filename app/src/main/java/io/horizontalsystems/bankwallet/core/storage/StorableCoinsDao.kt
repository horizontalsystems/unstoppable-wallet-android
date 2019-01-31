package io.horizontalsystems.bankwallet.core.storage

import android.arch.persistence.room.*
import io.horizontalsystems.bankwallet.entities.StorableCoin
import io.reactivex.Flowable

@Dao
abstract class StorableCoinsDao {

    @Query("SELECT * FROM StorableCoin WHERE coinCode = :coinCode")
    abstract fun getCoin(coinCode: String): Flowable<StorableCoin>

    @Query("SELECT * FROM StorableCoin ORDER BY `coinTitle` ASC")
    abstract fun getAllCoins(): Flowable<List<StorableCoin>>

    @Query("SELECT * FROM StorableCoin WHERE enabled = 1 ORDER BY `order` ASC")
    abstract fun getEnabledCoin(): Flowable<List<StorableCoin>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(storableCoin: StorableCoin)

    @Query("DELETE FROM StorableCoin")
    abstract fun deleteAll()

    @Query("UPDATE StorableCoin SET enabled = 0, `order` = null")
    protected abstract fun resetCoinsState()

    @Transaction
    open fun setEnabledCoins(coins: List<StorableCoin>) {
        resetCoinsState()
        coins.forEach { insert(it) }
    }

}
