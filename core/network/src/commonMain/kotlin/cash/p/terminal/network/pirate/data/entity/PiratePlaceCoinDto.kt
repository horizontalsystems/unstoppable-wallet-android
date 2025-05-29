package cash.p.terminal.network.pirate.data.entity

import cash.p.terminal.network.data.serializers.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
internal data class PiratePlaceCoinDto(
    val rank: Int?,
    val id: String,
    val name: String,
    val symbol: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("circulating_supply")
    val circulatingSupply: BigDecimal?,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("total_supply")
    val totalSupply: BigDecimal,
    @SerialName("max_supply")
    val maxSupply: Double?,
    val changes: ChangesDto,
    @SerialName("market_cap")
    val marketCap: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>?,
    val image: String,
    val price: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>,
    val description: Map<String, String>,
    val links: LinksDto,
    val ath: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>,
    @SerialName("ath_percentage")
    val athPercentage: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>,
    @SerialName("high_24h")
    val high24h: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>,
    @SerialName("low_24h")
    val low24h: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>,
    @SerialName("fully_diluted_valuation")
    val fullyDilutedValuation: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>,
    @SerialName("community_data")
    val communityData: CommunityDataDto,
    val graphs: Map<String, GraphUrlsDto>,
    @SerialName("is_active")
    val isActive: Boolean,
    @SerialName("is_currency")
    val isCurrency: Boolean,
    @SerialName("is_real_currency")
    val isRealCurrency: Boolean,
    @SerialName("updated_at")
    val updatedAt: String
)
