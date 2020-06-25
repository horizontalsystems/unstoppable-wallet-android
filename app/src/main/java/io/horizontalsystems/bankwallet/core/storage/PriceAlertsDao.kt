package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.PriceAlert

@Dao
interface PriceAlertsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(priceAlert: PriceAlert)

    @Query("SELECT * FROM PriceAlert")
    fun all(): List<PriceAlert>

    @Query("SELECT * FROM PriceAlert WHERE coinCode = :coinCode")
    fun priceAlert(coinCode: String): PriceAlert?

    @Query("DELETE FROM PriceAlert WHERE coinCode IN(:coinCodes)")
    fun delete(coinCodes: List<String>)

    @Query("SELECT COUNT(*) FROM PriceAlert")
    fun count(): Int

}
