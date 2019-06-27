package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.reactivex.Flowable

@Dao
interface EnabledWalletsDao {

    @Query("SELECT * FROM EnabledWallet ORDER BY `walletOrder` ASC")
    fun getEnabledCoins(): Flowable<List<EnabledWallet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(storableCoin: EnabledWallet)

    @Query("DELETE FROM EnabledWallet")
    fun deleteAll()

    @Transaction
    fun insertCoins(coins: List<EnabledWallet>) {
        coins.forEach { insert(it) }
    }
}
