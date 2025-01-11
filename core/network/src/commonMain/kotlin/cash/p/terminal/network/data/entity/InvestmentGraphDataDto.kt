package cash.p.terminal.network.data.entity

import cash.p.terminal.network.data.serializers.ISO8601InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
internal data class InvestmentGraphDataDto(
    @SerialName("points")
    val points: List<PricePointDto>
)

@Serializable
internal data class PricePointDto(
    val value: Double,
    val balance: Double,
    @Serializable(with = ISO8601InstantSerializer::class)
    val from: Instant,
    @Serializable(with = ISO8601InstantSerializer::class)
    val to: Instant,
    val price: Map<String, String>,
    @SerialName("balance_price")
    val balancePrice: Map<String, String>
)
