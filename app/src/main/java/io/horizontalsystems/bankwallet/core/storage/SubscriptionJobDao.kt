package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.entities.SubscriptionJob

@Dao
interface SubscriptionJobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(subscriptionJob: SubscriptionJob)

    @Query("SELECT * FROM SubscriptionJob")
    fun all(): List<SubscriptionJob>

    @Query("SELECT * FROM SubscriptionJob WHERE coinCode = :coinCode")
    fun jobsByCoinCode(coinCode: String): List<SubscriptionJob>

    @Delete()
    fun delete(subscriptionJob: SubscriptionJob)

}
