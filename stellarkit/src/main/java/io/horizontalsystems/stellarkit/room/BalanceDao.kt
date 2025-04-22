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

    @Query("SELECT * FROM AssetNativeBalance LIMIT 0, 1")
    fun getNativeBalance(): AssetNativeBalance?
}
