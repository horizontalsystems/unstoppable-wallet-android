package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.PriceAlertRecord

@Dao
interface PriceAlertsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(priceAlert: PriceAlertRecord)

    @Query("SELECT * FROM PriceAlertRecord")
    fun all(): List<PriceAlertRecord>

    @Query("DELETE FROM PriceAlertRecord WHERE coinCode IN(:coinCodes)")
    fun delete(coinCodes: List<String>)

    @Query("SELECT COUNT(*) FROM PriceAlertRecord")
    fun count(): Int

    @Query("DELETE FROM PriceAlertRecord WHERE coinCode NOT IN(:coinCodes)")
    fun deleteExcluding(coinCodes: List<String>)
}
