package cash.p.terminal.network.pirate.data.entity

import cash.p.terminal.network.data.serializers.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
internal data class MarketCapDto(
    @SerialName("value_24h")
    val value24h: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>,
)
