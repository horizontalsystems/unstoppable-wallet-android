package cash.p.terminal.network.pirate.data.entity

import cash.p.terminal.network.data.serializers.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
internal class MarketTickerDto(
    @SerialName("from_symbol")
    val fromSymbol: String?,
    @SerialName("target_symbol")
    val targetSymbol: String?,
    @SerialName("target_id")
    val targetId: String?,
    val market: String?,
    @SerialName("market_url")
    val marketUrl: String,
    @SerialName("trade_url")
    val tradeUrl: String?,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal?,
    @SerialName("price_usd")
    @Serializable(with = BigDecimalSerializer::class)
    val priceUsd: BigDecimal?,
    @Serializable(with = BigDecimalSerializer::class)
    val volume: BigDecimal?,
    @SerialName("volume_usd")
    @Serializable(with = BigDecimalSerializer::class)
    val volumeUsd: BigDecimal?,
    @SerialName("volume_percent")
    @Serializable(with = BigDecimalSerializer::class)
    val volumePercent: BigDecimal?,
    @SerialName("trust_score")
    val trustScore: String?
)
