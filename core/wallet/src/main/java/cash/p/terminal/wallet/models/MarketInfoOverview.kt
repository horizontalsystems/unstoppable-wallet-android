package cash.p.terminal.wallet.models

import cash.p.terminal.wallet.entities.FullCoin
import io.horizontalsystems.core.models.HsTimePeriod
import java.math.BigDecimal
import java.util.*

data class MarketInfoOverview(
    val fullCoin: FullCoin,
    val marketCap: BigDecimal?,
    val marketCapRank: Int?,
    val totalSupply: BigDecimal?,
    val circulatingSupply: BigDecimal?,
    val volume24h: BigDecimal?,
    val dilutedMarketCap: BigDecimal?,
    val tvl: BigDecimal?,
    val performance: Map<String, Map<HsTimePeriod, BigDecimal>>,
    val genesisDate: Date?,
    val categories: List<CoinCategory>,
    val description: String,
    val links: Map<LinkType, String>,
)
