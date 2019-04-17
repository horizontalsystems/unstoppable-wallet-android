package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.StorableCoin
import io.reactivex.Flowable

@Dao
interface StorableCoinsDao {

    @Query("SELECT * FROM StorableCoin WHERE enabled = 1 ORDER BY `order` ASC")
    fun getEnabledCoin(): Flowable<List<StorableCoin>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(storableCoin: StorableCoin)

    @Query("DELETE FROM StorableCoin")
    fun deleteAll()

    @Query("DELETE FROM StorableCoin WHERE coinCode IN (:codes)")
    fun deleteCoins(codes: List<String>)

    @Query("UPDATE StorableCoin SET enabled = 0, `order` = null")
    fun resetCoinsState()

    @Transaction
    fun setEnabledCoins(coins: List<StorableCoin>) {
        resetCoinsState()
        coins.forEach { insert(it) }
    }

    @Transaction
    fun insertCoins(coins: List<StorableCoin>) {
        coins.forEach { insert(it) }
    }

    @Transaction
    fun bulkUpdate(inserted: List<Coin>, deleted: List<Coin>) {
        insertCoins(inserted.map { StorableCoin(it.code, it.title, it.type, false, null) })
        deleteCoins(deleted.map { it.code })
    }
}
