package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.entities.PriceAlert

@Dao
interface PriceAlertsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(priceAlert: PriceAlert)

    @Query("SELECT * FROM PriceAlert")
    fun all(): List<PriceAlert>

    @Query("SELECT * FROM PriceAlert WHERE coinId = :coinId")
    fun priceAlert(coinId: String): PriceAlert?

    @Query("SELECT COUNT(*) FROM PriceAlert")
    fun count(): Int

    @Query("DELETE FROM PriceAlert")
    fun deleteAll()

    @Delete()
    fun delete(it: PriceAlert)

}
