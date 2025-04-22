package io.horizontalsystems.stellarkit.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface BalanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNative(balance: AssetNativeBalance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(balance: AssetBalance)
}
