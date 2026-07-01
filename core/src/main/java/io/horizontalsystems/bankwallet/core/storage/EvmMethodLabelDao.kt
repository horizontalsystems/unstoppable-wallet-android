package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.entities.EvmMethodLabel

@Dao
interface EvmMethodLabelDao {

    @Query("SELECT * FROM EvmMethodLabel WHERE methodId = :methodId")
    fun get(methodId: String): EvmMethodLabel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(label: EvmMethodLabel)

    @Query("DELETE FROM EvmMethodLabel")
    fun clear()

    @Transaction
    fun update(labels: List<EvmMethodLabel>) {
        clear()
        labels.forEach { insert(it) }
    }

}
