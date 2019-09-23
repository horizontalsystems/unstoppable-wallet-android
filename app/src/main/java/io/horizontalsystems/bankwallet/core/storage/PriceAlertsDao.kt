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

    @Query("DELETE FROM PriceAlertRecord WHERE coinCode == :coinCode")
    fun delete(coinCode: String)

    @Query("SELECT COUNT(*) FROM PriceAlertRecord")
    fun count(): Int
}
