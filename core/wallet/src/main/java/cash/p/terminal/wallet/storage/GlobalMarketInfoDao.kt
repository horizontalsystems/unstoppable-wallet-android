package cash.p.terminal.wallet.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cash.p.terminal.wallet.models.GlobalMarketInfo
import io.horizontalsystems.core.models.HsTimePeriod

@Dao
interface GlobalMarketInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(globalMarketInfo: GlobalMarketInfo)

    @Query("SELECT * FROM GlobalMarketInfo WHERE currencyCode=:currencyCode AND timePeriod=:timePeriod")
    fun getGlobalMarketInfo(currencyCode: String, timePeriod: HsTimePeriod): GlobalMarketInfo?
}
