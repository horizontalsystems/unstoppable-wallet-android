package cash.p.terminal.network.pirate.domain.enity

import java.math.BigDecimal

data class PiratePlaceCoin(
    val rank: Int?,
    val id: String,
    val name: String,
    val symbol: String,
    val circulatingSupply: BigDecimal,
    val totalSupply: BigDecimal,
    val maxSupply: Double?,
    val changes: Changes,
    val marketCap: Map<String, BigDecimal>,
    val image: String,
    val price: Map<String, BigDecimal>,
    val description: Map<String, String>,
    val links: Links,
    val ath: Map<String, BigDecimal>,
    val athPercentage: Map<String, BigDecimal>,
    val high24h: Map<String, BigDecimal>,
    val low24h: Map<String, BigDecimal>,
    val communityData: CommunityData,
    val graphs: Map<String, GraphUrls>,
    val isActive: Boolean,
    val isCurrency: Boolean,
    val isRealCurrency: Boolean,
    val updatedAt: String,
    val fullyDilutedValuation: Map<String, BigDecimal>,
)
