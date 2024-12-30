package cash.p.terminal.network.data.entity

import cash.p.terminal.network.data.serializers.ISO8601InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
internal data class StakeDataDto(
    val stakes: List<StakeDto>
)

@Serializable
data class StakeDto(
    val id: Long,
    val type: String,
    val balance: Double,
    val amount: Double,
    @Serializable(with = ISO8601InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("balance_price")
    val balancePrice: Map<String, String>,

    @SerialName("amount_price")
    val amountPrice: Map<String, String>
)