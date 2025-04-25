package io.horizontalsystems.stellarkit.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BalanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNative(balance: AssetNativeBalance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(balance: AssetBalance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(balances: List<AssetBalance>)

    @Query("SELECT * FROM AssetNativeBalance LIMIT 0, 1")
    fun getNativeBalance(): AssetNativeBalance?

    @Query("SELECT * FROM AssetBalance")
    fun getAssetBalances(): List<AssetBalance>
}
