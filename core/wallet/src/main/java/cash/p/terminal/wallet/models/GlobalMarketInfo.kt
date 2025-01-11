package cash.p.terminal.wallet.models

import androidx.room.Entity
import io.horizontalsystems.core.models.HsTimePeriod

@Entity(primaryKeys = ["currencyCode", "timePeriod"])
class GlobalMarketInfo(
    val currencyCode: String,
    val timePeriod: HsTimePeriod,
    val points: List<GlobalMarketPoint>,
    val timestamp: Long
)
